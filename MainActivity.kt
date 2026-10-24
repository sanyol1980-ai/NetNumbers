package com.example.netnumbers

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { Surface(Modifier.fillMaxSize()) { App() } } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    val scope = rememberCoroutineScope()
    val logs = remember { mutableStateListOf<String>() }
    val time = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val engine = remember { NetworkEngine(scope) { logs.add(0, "${time.format(Date())}  $it") } }
    DisposableEffect(Unit) { onDispose { engine.stopServer() } }

    var protocol by remember { mutableStateOf(Protocol.TCP) }
    var serverMode by remember { mutableStateOf(false) }
    var running by remember { mutableStateOf(false) }
    var host by remember { mutableStateOf("192.168.1.100") }
    var port by remember { mutableStateOf("5000") }
    var number by remember { mutableStateOf("") }
    val myIp = remember { localIpAddress() }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Передача чисел по сети", style = MaterialTheme.typography.titleLarge)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Protocol.values().forEach { p ->
                FilterChip(selected = protocol == p, enabled = !running,
                    onClick = { protocol = p }, label = { Text(p.name) })
            }
            Spacer(Modifier.width(8.dp))
            FilterChip(selected = !serverMode, enabled = !running,
                onClick = { serverMode = false }, label = { Text("Клиент") })
            FilterChip(selected = serverMode, enabled = !running,
                onClick = { serverMode = true }, label = { Text("Сервер") })
        }

        if (serverMode) Text("IP этого телефона: $myIp")

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!serverMode) {
                OutlinedTextField(host, { host = it }, label = { Text("IP сервера") },
                    singleLine = true, modifier = Modifier.weight(2f))
            }
            OutlinedTextField(port, { port = it.filter(Char::isDigit) }, label = { Text("Порт") },
                singleLine = true, modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        }

        val portInt = port.toIntOrNull()
        if (serverMode) {
            Button(
                enabled = portInt != null && portInt in 1..65535,
                onClick = {
                    if (running) engine.stopServer() else engine.startServer(protocol, portInt!!)
                    running = !running
                }
            ) { Text(if (running) "Остановить сервер" else "Запустить сервер") }
        } else {
            OutlinedTextField(number, { number = it }, label = { Text("Число") },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    enabled = portInt != null && number.toDoubleOrNull() != null,
                    onClick = { engine.send(protocol, host.trim(), portInt!!, number.trim()) }
                ) { Text("Отправить") }
                OutlinedButton(
                    enabled = portInt != null,
                    onClick = {
                        number = "%.2f".format(Locale.US, Random.nextDouble(0.0, 100.0))
                        engine.send(protocol, host.trim(), portInt!!, number)
                    }
                ) { Text("Случайное число") }
            }
        }

        HorizontalDivider()
        Text("Журнал", style = MaterialTheme.typography.titleMedium)
        LazyColumn(Modifier.weight(1f)) { items(logs) { Text(it, style = MaterialTheme.typography.bodySmall) } }
    }
}
