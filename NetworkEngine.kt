package com.example.netnumbers

import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.*
import java.util.concurrent.CopyOnWriteArrayList

enum class Protocol { TCP, UDP }

/** Сетевая логика: сервер и клиент для TCP и UDP. Данные — числа в текстовом виде. */
class NetworkEngine(
    private val scope: CoroutineScope,
    private val onLog: (String) -> Unit
) {
    private var serverJob: Job? = null
    private var tcpServer: ServerSocket? = null
    private var udpServer: DatagramSocket? = null
    private val clients = CopyOnWriteArrayList<Socket>()

    private fun log(msg: String) {
        scope.launch(Dispatchers.Main) { onLog(msg) }
    }

    // ---------------- СЕРВЕР ----------------

    fun startServer(protocol: Protocol, port: Int) {
        stopServer()
        serverJob = scope.launch(Dispatchers.IO) {
            try {
                when (protocol) {
                    Protocol.TCP -> runTcpServer(port)
                    Protocol.UDP -> runUdpServer(port)
                }
            } catch (e: Exception) {
                if (isActive) log("Ошибка сервера: ${e.message}")
            }
        }
    }

    fun stopServer() {
        serverJob?.cancel()
        serverJob = null
        runCatching { tcpServer?.close() }
        runCatching { udpServer?.close() }
        clients.forEach { runCatching { it.close() } }
        clients.clear()
        tcpServer = null
        udpServer = null
    }

    private fun runTcpServer(port: Int) {
        val ss = ServerSocket(port)
        tcpServer = ss
        log("TCP-сервер запущен, порт $port")
        while (!ss.isClosed) {
            val client = ss.accept()
            clients.add(client)
            scope.launch(Dispatchers.IO) { handleTcpClient(client) }
        }
    }

    private fun handleTcpClient(client: Socket) {
        val who = client.remoteSocketAddress.toString()
        log("TCP: подключился $who")
        try {
            val reader = BufferedReader(InputStreamReader(client.getInputStream()))
            val out = client.getOutputStream()
            while (true) {
                val line = reader.readLine() ?: break
                log("TCP ← $who: $line")
                out.write("ACK $line\n".toByteArray())   // ответ клиенту
                out.flush()
            }
        } catch (_: Exception) {
        } finally {
            runCatching { client.close() }
            clients.remove(client)
            log("TCP: отключился $who")
        }
    }

    private fun runUdpServer(port: Int) {
        val socket = DatagramSocket(port)
        udpServer = socket
        log("UDP-сервер запущен, порт $port")
        val buf = ByteArray(1024)
        while (!socket.isClosed) {
            val packet = DatagramPacket(buf, buf.size)
            socket.receive(packet)
            val text = String(packet.data, 0, packet.length).trim()
            log("UDP ← ${packet.address.hostAddress}:${packet.port}: $text")
            val reply = "ACK $text".toByteArray()
            socket.send(DatagramPacket(reply, reply.size, packet.address, packet.port))
        }
    }

    // ---------------- КЛИЕНТ ----------------

    fun send(protocol: Protocol, host: String, port: Int, value: String) {
        scope.launch(Dispatchers.IO) {
            try {
                when (protocol) {
                    Protocol.TCP -> sendTcp(host, port, value)
                    Protocol.UDP -> sendUdp(host, port, value)
                }
            } catch (e: Exception) {
                log("Ошибка отправки: ${e.message}")
            }
        }
    }

    private fun sendTcp(host: String, port: Int, value: String) {
        Socket().use { s ->
            s.connect(InetSocketAddress(host, port), 3000)
            s.soTimeout = 2000
            s.getOutputStream().apply { write("$value\n".toByteArray()); flush() }
            log("TCP → $host:$port: $value")
            val reply = runCatching {
                BufferedReader(InputStreamReader(s.getInputStream())).readLine()
            }.getOrNull()
            log(if (reply != null) "TCP ← ответ: $reply" else "TCP: ответа нет")
        }
    }

    private fun sendUdp(host: String, port: Int, value: String) {
        DatagramSocket().use { s ->
            s.soTimeout = 2000
            val data = value.toByteArray()
            s.send(DatagramPacket(data, data.size, InetAddress.getByName(host), port))
            log("UDP → $host:$port: $value")
            val buf = ByteArray(1024)
            val packet = DatagramPacket(buf, buf.size)
            try {
                s.receive(packet)
                log("UDP ← ответ: ${String(packet.data, 0, packet.length)}")
            } catch (_: SocketTimeoutException) {
                log("UDP: ответа нет (для UDP это нормально)")
            }
        }
    }
}

/** Локальный IPv4-адрес телефона (Wi-Fi). */
fun localIpAddress(): String =
    NetworkInterface.getNetworkInterfaces().toList()
        .flatMap { it.inetAddresses.toList() }
        .firstOrNull { !it.isLoopbackAddress && it is Inet4Address }
        ?.hostAddress ?: "не найден"
