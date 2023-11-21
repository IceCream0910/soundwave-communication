package com.icecream.soundwave

import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import co.euphony.rx.AcousticSensor
import co.euphony.rx.EuRxManager
import co.euphony.tx.EuTxManager
import com.icecream.soundwave.ui.theme.SoundwaveTheme

class MainActivity : ComponentActivity() {
    // Create instances of EuTxManager and EuRxManager
    private val mTxManager = EuTxManager()
    private val mRxManager = EuRxManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SoundwaveTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Pass EuTxManager to the transmitterLayout
                    transmitterLayout(mTxManager)

                    // Pass EuRxManager to the receiverLayout
                    receiverLayout(mRxManager)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun transmitterLayout(txManager: EuTxManager, modifier: Modifier = Modifier) {
    var text by remember { mutableStateOf("") }
    var isSpeaking by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(10.dp)
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it
            },
            label = { Text("전송할 텍스트 입력") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        Button(
            enabled = !isSpeaking,
            onClick = {
                val textValue = text.toString()

                if (textValue.isNotEmpty()) {
                    isSpeaking = true
                    if (textValue.length <= 30) {
                        txManager.euInitTransmit(textValue)
                        txManager.process(1)
                        isSpeaking = false
                    } else {
                        val chunkSize = 30
                        val numberOfChunks = (textValue.length + chunkSize - 1) / chunkSize
                        var currentIndex = 0

                        val handler = Handler()

                        fun processNextChunk() {
                            if (currentIndex < numberOfChunks) {
                                val start = currentIndex * chunkSize
                                val end = minOf((currentIndex + 1) * chunkSize, textValue.length)
                                val chunk = textValue.substring(start, end)
                                Log.e("taein", chunk)

                                txManager.euInitTransmit(chunk)
                                txManager.process(1)

                                currentIndex++

                                // Post a delayed message to process the next chunk after 8 seconds
                                handler.postDelayed({ processNextChunk() }, 7000)
                                isSpeaking = false
                            }
                        }

                        // Start processing the first chunk
                        processNextChunk()
                    }
                }
            }
            ,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(text = if (isSpeaking) "전송중" else "전송")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun receiverLayout(rxManager: EuRxManager, modifier: Modifier = Modifier) {
    var receivedText by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(10.dp)
    ) {
        Text(text = receivedText) // 수신된 텍스트

        Button(
            onClick = {
                if (!isListening) {
                    rxManager.listen()
                    isListening = true

                    rxManager.setAcousticSensor(AcousticSensor {
                        receivedText = receivedText + it
                        rxManager.finish()
                        rxManager.listen() //재시작
                    })
                } else {
                    rxManager.finish()
                    isListening = false
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(top = 20.dp)
        ) {
            Text(text = if (isListening) "수신 정지" else "수신 시작")
        }
    }
}



@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SoundwaveTheme {
        transmitterLayout(EuTxManager())
        receiverLayout(EuRxManager())
    }
}