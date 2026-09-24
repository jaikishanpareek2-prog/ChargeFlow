package com.chargeanim.pro.media

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.ImageLoader
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.chargeanim.pro.data.MediaSelection
import com.chargeanim.pro.data.MediaType

@Composable fun MediaRenderer(selection: MediaSelection, modifier: Modifier = Modifier, fallback: @Composable () -> Unit) {
    if (selection.uri == null || selection.type == MediaType.NONE) { Box(modifier, contentAlignment = Alignment.Center) { fallback() }; return }
    when (selection.type) { MediaType.LOTTIE -> LottieRenderer(selection.uri, modifier); MediaType.GIF -> GifRenderer(selection.uri, modifier); MediaType.MP4 -> Mp4Renderer(selection.uri, modifier); MediaType.NONE -> Box(modifier, contentAlignment = Alignment.Center) { fallback() } }
}
@Composable private fun LottieRenderer(uriString:String,modifier:Modifier){ val composition by rememberLottieComposition(LottieCompositionSpec.Url(uriString)); LottieAnimation(composition,iterations=LottieConstants.IterateForever,modifier=modifier) }
@Composable private fun GifRenderer(uriString:String,modifier:Modifier){ val context=LocalContext.current; val imageLoader=remember{ImageLoader.Builder(context).components{if(android.os.Build.VERSION.SDK_INT>=28)add(ImageDecoderDecoder.Factory())else add(GifDecoder.Factory())}.build()}; AsyncImage(Uri.parse(uriString),contentDescription=null,imageLoader=imageLoader,modifier=modifier) }
@Composable private fun Mp4Renderer(uriString:String,modifier:Modifier){val context=LocalContext.current;val player=remember{ExoPlayer.Builder(context).build().apply{setMediaItem(MediaItem.fromUri(Uri.parse(uriString)));repeatMode=androidx.media3.common.Player.REPEAT_MODE_ONE;volume=0f;prepare();playWhenReady=true}};DisposableEffect(Unit){onDispose{player.release()}};AndroidView(modifier=modifier,factory={PlayerView(it).apply{useController=false;this.player=player}})}
@Composable fun MediaFallbackBolt(tint:androidx.compose.ui.graphics.Color=MaterialTheme.colorScheme.primary){Icon(androidx.compose.material.icons.Icons.Filled.Bolt,null,tint=tint,modifier=Modifier.fillMaxSize())}