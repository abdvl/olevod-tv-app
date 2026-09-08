from pathlib import Path
import subprocess,time,json,os,hashlib,datetime
# Run from repository root after installing the debug APK. All routes explicitly use preview=true.
adb=str((Path(os.environ.get('ANDROID_HOME','.tools/android-sdk'))/'platform-tools/adb').resolve())
shots=Path('docs/verification/v2/screenshots');shots.mkdir(parents=True,exist_ok=True)
records=[]
def cmd(*args):return subprocess.run([adb,'-s','emulator-5554',*args],check=True,stdout=subprocess.PIPE).stdout
def start(screen):
 cmd('shell','am','force-stop','com.olevod.tv');cmd('shell','am','start','-n','com.olevod.tv/.MainActivity','--ez','preview','true','--es','screen',screen);time.sleep(1.4)
def key(*codes):
 for code in codes:cmd('shell','input','keyevent',str(code));time.sleep(.24)
def shot(name,state):
 time.sleep(.4);(shots/(name+'.png')).write_bytes(cmd('exec-out','screencap','-p'));records.append({'file':name+'.png','state':state});print(name,flush=True)
start('home');shot('home-default','public fixtures; five synthetic records; Home focus');key(20);shot('home-recent-focused','first recent expanded');key(19,4);shot('exit-confirmation','default continue');key(4)
key(*([20]*30));shot('home-latest-bottom','last update row focused, complete card and metadata')
start('category');key(20);shot('mini-category','movie mini home, first ranked card')
start('browse');shot('catalog','catalog first sort trigger');key(22,22,22,23);shot('catalog-year','year modal');key(4,22,23);shot('catalog-more','draft membership and initial modal')
start('search');key(20,20,20,20,23,22,23);time.sleep(.6);shot('search-suggestion','MN literal with preview suggestions');key(22,22,22,22,22);time.sleep(.3);key(23);time.sleep(1);shot('search-results','confirmed first public fixture title; first result focus')
start('player');shot('player-normal','virtual video, actual PlayerContent');key(19);shot('player-video-focus','video region focus');key(23);shot('player-full-visible','virtual paused video; controls overlay');key(19);shot('player-full-hidden','same virtual video; overlay hidden');key(20,20,22,22,23,20);shot('player-episodes','21-30 preview group; first item focused');key(19,19);key(22,22,22,22,22,22,23);shot('player-speed','speed modal')
start('history');key(20);shot('history-device','synthetic current-account records; first resume focus');key(19,22,23,20);shot('history-cloud','synthetic cloud records without dates or deletion')
start('favorites');key(20);shot('favorites','public fixture collection; first poster focus')
start('account');time.sleep(1);shot('login-remembered','synthetic movie_fan; public CAPTCHA, no credential submission')
key(23,23,23,23,20,20,20,20,23);time.sleep(1);shot('login-error','fixture submission deliberately fails; input and saved credentials retained');start('account');time.sleep(1);key(21,22,23,22,23);shot('login-first','fixture saved credentials cleared, first entry')
# Emulator setting is restored even if a capture fails.
font=cmd('shell','settings','get','system','font_scale').decode().strip()
try:
 cmd('shell','settings','put','system','font_scale','1.3');start('player');shot('large-font','fontScale 1.3 ordinary player; eight controls and episodes');start('history');key(20,20);shot('history-large-font','fontScale 1.3 focused second row')
finally:cmd('shell','settings','put','system','font_scale',font if font!='null' else '1.0')
Path('.tools/v2-capture-states.json').write_text(json.dumps(records,ensure_ascii=False,indent=2))
start('home')
