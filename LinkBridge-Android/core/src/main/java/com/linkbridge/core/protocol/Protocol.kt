package com.linkbridge.core.protocol
import java.nio.ByteBuffer
import java.util.zip.CRC32
object Protocol { const val VERSION:Byte=1; const val MAX_BLE_PAYLOAD=180
 enum class Type(val id:Byte){HELLO(1),AUTH(2),TELEMETRY(3),COMMAND(4),FILE_META(5),FILE_CHUNK(6),ACK(7),FIND(8),STOP_FIND(9)}
 data class Frame(val type:Type,val sequence:Int,val payload:ByteArray)
 fun encode(f:Frame):ByteArray { val crc=CRC32().apply{update(f.payload)}.value.toInt();return ByteBuffer.allocate(12+f.payload.size).put(VERSION).put(f.type.id).putInt(f.sequence).putShort(f.payload.size.toShort()).putInt(crc).put(f.payload).array() }
 fun decode(b:ByteArray):Frame { require(b.size>=12);val x=ByteBuffer.wrap(b);require(x.get()==VERSION);val t=Type.entries.first{it.id==x.get()};val s=x.int;val n=x.short.toInt() and 0xffff;val crc=x.int;require(n==x.remaining());val p=ByteArray(n);x.get(p);require(CRC32().apply{update(p)}.value.toInt()==crc);return Frame(t,s,p) }
}
