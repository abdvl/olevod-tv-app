package com.olevod.tv

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text

@Composable
fun ConnectedHome(state:HomeState,vm:AppViewModel,open:(Movie)->Unit,browse:(String)->Unit) {
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(40.dp,14.dp,40.dp,30.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
        if(state.loading)item{Text("正在为你寻找好故事…",color=Muted,fontSize=18.sp)}
        state.error?.let{item{ErrorNotice(it){vm.loadHome()}}}
        if(state.heroes.isNotEmpty())item{Row(horizontalArrangement=Arrangement.spacedBy(16.dp)){state.heroes.take(2).forEachIndexed{i,h->HeroCard(h,Modifier.weight(if(i==0)1.6f else 1f)){open(Movie(h.id,h.title,h.image,h.note))}}}}
        items(state.sections,key={it.category.id}) { section ->
            when {
                section.error!=null->Column{SectionHeading(section.category.name);ErrorNotice(section.error){vm.retrySection(section.category)}}
                section.loading->Column{SectionHeading(section.category.name);Text("正在加载…",color=Muted)}
                section.movies.isEmpty()->Column{SectionHeading(section.category.name);Text("暂时没有影片",color=Muted)}
                else->HomeMovieGroup(section.movies,open,section.category.name){browse(section.category.name)}
            }
        }
    }
}
@Composable internal fun ErrorNotice(message:String,retry:()->Unit){Column(verticalArrangement=Arrangement.spacedBy(10.dp)){Text(message,color=Muted,fontSize=15.sp);TvAction("重试",selected=true,onClick=retry)}}
