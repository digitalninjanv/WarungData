package com.warungdata.pos.core.printer

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import com.warungdata.pos.core.database.AppDatabase
import com.warungdata.pos.core.database.entity.SaleEntity
import com.warungdata.pos.core.database.entity.SaleItemEntity
import com.warungdata.pos.core.datastore.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

class PrinterService(private val context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val settingsRepo = SettingsRepository(context)

    suspend fun printStruk(saleId: Int, address: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val sale = db.saleDao().getSaleById(saleId) ?: return@withContext Result.failure(Exception("Transaksi tidak ditemukan"))
            val items = db.saleItemDao().getItemsBySaleIdOnce(saleId)
            val store = settingsRepo.storeName.first()

            val text = buildStrukText(sale, items, if (store.isBlank()) null else store)

            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val adapter = bluetoothManager.adapter
                ?: return@withContext Result.failure(Exception("Bluetooth tidak tersedia"))

            val device = adapter.getRemoteDevice(address)
            val socket: BluetoothSocket = device.createRfcommSocketToServiceRecord(
                UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
            )
            adapter.cancelDiscovery()
            socket.connect()
            val output: OutputStream = socket.outputStream
            output.write(text.toByteArray(Charsets.UTF_8))
            output.write(byteArrayOf(0x1D, 0x56, 0x42, 0x00)) // cut paper
            output.flush()
            socket.close()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildStrukText(sale: SaleEntity, items: List<SaleItemEntity>, store: String?): String {
        val dateFmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("id", "ID"))
        val sb = StringBuilder()
        val line = "=".repeat(32)

        sb.appendLine(line)
        sb.appendLine(center(store ?: "WarungData POS"))
        sb.appendLine(line)
        sb.appendLine("No: ${sale.invoiceNumber}")
        sb.appendLine("Tgl: ${dateFmt.format(Date(sale.date))}")
        sb.appendLine("Kasir: ${sale.cashierName}")
        sb.appendLine("-".repeat(32))
        sb.appendLine(String.format("%-18s %4s %10s", "Item", "Qty", "Harga"))
        sb.appendLine("-".repeat(32))

        for (item in items) {
            val name = item.productName.take(16)
            sb.appendLine(String.format("%-18s %4d %10d", name, item.qty, item.subtotal))
        }

        sb.appendLine("-".repeat(32))
        sb.appendLine(String.format("%-22s %10d", "Subtotal:", sale.subtotal))
        if (sale.discount > 0) {
            sb.appendLine(String.format("%-22s %10d", "Diskon:", sale.discount))
        }
        sb.appendLine(String.format("%-22s %10d", "Total:", sale.total))
        sb.appendLine("Metode: ${sale.paymentMethod}")
        sb.appendLine(line)
        sb.appendLine(center("Terima Kasih"))
        sb.appendLine(center("Barang yang sudah dibeli"))
        sb.appendLine(center("tidak dapat ditukar/kembali"))
        sb.appendLine()
        sb.appendLine()
        sb.appendLine()

        return sb.toString()
    }

    private fun center(text: String, width: Int = 32): String {
        val padding = (width - text.length) / 2
        return " ".repeat(maxOf(0, padding)) + text
    }
}
