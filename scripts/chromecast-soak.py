#!/usr/bin/env python3
"""Observe an already-playing TV app without key events or account/media URL logging.

Keep the app's playback controls visible so fresh UI dumps expose actual player time.
This is an opt-in diagnostic, not an audio/visual quality or frame-drop benchmark.
"""
import argparse
import datetime
import json
import re
import subprocess
import time
import xml.etree.ElementTree as ET
from pathlib import Path


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--adb', default='adb')
    parser.add_argument('--serial', required=True)
    parser.add_argument('--minutes', type=float, default=30)
    parser.add_argument('--interval', type=float, default=30)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    if args.minutes <= 0 or args.interval < 5:
        parser.error('Use positive minutes and an interval of at least five seconds')
    args.output.parent.mkdir(parents=True, exist_ok=True)

    def adb(*command):
        return subprocess.check_output([args.adb, '-s', args.serial, *command],
                                       text=True, timeout=25, stderr=subprocess.DEVNULL)

    device_start = int(adb('shell', 'date', '+%s').strip())

    def read_sample():
        sample = {'utc': datetime.datetime.now(datetime.timezone.utc).isoformat()}
        try:
            sample['pid'] = adb('shell', 'pidof', 'com.olevod.tv').strip()
            sessions = adb('shell', 'dumpsys', 'media_session')
            active = False
            for line in sessions.splitlines():
                if re.match(r'\s+\S+ .*userId=', line):
                    active = 'com.olevod.tv' in line
                if 'package=com.olevod.tv' in line:
                    active = True
                if active and 'state=PlaybackState' in line:
                    state = re.search(r'\{state=(?:[A-Z_]+\()?([0-9]+)', line)
                    if state:
                        sample['playback_state'] = int(state.group(1))
                    for key in ['position', 'buffered position', 'speed', 'updated']:
                        value = re.search(r', ' + key + r'=([\d.\-]+)', line)
                        if value:
                            sample[key.replace(' ', '_')] = float(value.group(1))
                    break
            activity = adb('shell', 'dumpsys', 'activity', 'activities')
            sample['app_resumed'] = any('com.olevod.tv/' in line and 'ResumedActivity' in line
                                        for line in activity.splitlines())
            memory = adb('shell', 'dumpsys', 'meminfo', 'com.olevod.tv')
            pss = re.search(r'TOTAL PSS:\s*(\d+)', memory)
            if pss:
                sample['pss_kb'] = int(pss.group(1))
            # Each dump uses a new name. Never reuse an old hierarchy after a failed dump.
            remote = '/sdcard/olevod-soak-' + str(time.time_ns()) + '.xml'
            try:
                status = adb('shell', 'uiautomator', 'dump', '--compressed', remote)
                sample['ui_dump_ok'] = 'dumped to' in status
                if sample['ui_dump_ok']:
                    root = ET.fromstring(adb('shell', 'cat', remote))
                    labels = [node.get('text', '') for node in root.iter('node')]
                    times = [label for label in labels if re.fullmatch(r'(?:\d+:)?\d{2}:\d{2}', label)]
                    sample['ui_times'] = times
                    sample['ui_buffering'] = '正在缓冲…' in labels
                    sample['ui_pause_action'] = '暂停' in labels
                    if len(times) == 2:
                        values = [sum(int(part) * 60 ** index for index, part in enumerate(reversed(value.split(':'))))
                                  for value in times]
                        sample['ui_position_seconds'], sample['ui_duration_seconds'] = values
            finally:
                adb('shell', 'rm', '-f', remote)
            crashes = adb('logcat', '-b', 'crash', '-d', '-v', 'epoch', '-t', '300')
            sample['new_app_crash_lines'] = sum(1 for line in crashes.splitlines()
                if 'Process: com.olevod.tv,' in line and
                (stamp := re.match(r'\s*(\d+\.\d+)', line)) and float(stamp.group(1)) >= device_start)
        except Exception as error:
            # Exceptions can include command or transport text; report only their type.
            sample['observation_error'] = type(error).__name__
        return sample

    samples = []
    start = time.monotonic()
    deadline = start + args.minutes * 60
    next_sample = start
    with args.output.open('x') as output:
        while True:
            time.sleep(max(0, next_sample - time.monotonic()))
            sample = read_sample()
            sample['elapsed_seconds'] = round(time.monotonic() - start, 3)
            samples.append(sample)
            line = json.dumps(sample, ensure_ascii=False)
            output.write(line + '\n'); output.flush()
            print(line, flush=True)
            if next_sample >= deadline:
                break
            next_sample = min(deadline, next_sample + args.interval)
    positions = [sample for sample in samples if 'ui_position_seconds' in sample]
    summary = {
        'samples': len(samples),
        'elapsed_seconds': samples[-1]['elapsed_seconds'],
        'position_samples': len(positions),
        'actual_ui_advance_seconds': positions[-1]['ui_position_seconds'] - positions[0]['ui_position_seconds'] if len(positions) >= 2 else None,
        'unique_pids': len({sample['pid'] for sample in samples if sample.get('pid')}),
        'nonplaying_samples': sum(sample.get('playback_state') != 3 for sample in samples),
        'background_samples': sum(not sample.get('app_resumed') for sample in samples),
        'buffering_samples': sum(bool(sample.get('ui_buffering')) for sample in samples),
        'observation_errors': sum('observation_error' in sample for sample in samples),
        'new_app_crash_lines': max((sample.get('new_app_crash_lines', 0) for sample in samples), default=0),
        'scope': 'Sampled state and actual visible player clock; not continuous frame, audible sound, or zero-buffering proof',
    }
    args.output.with_suffix('.summary.json').write_text(json.dumps(summary, ensure_ascii=False, indent=2) + '\n')
    print(json.dumps(summary, ensure_ascii=False), flush=True)


if __name__ == '__main__':
    main()
