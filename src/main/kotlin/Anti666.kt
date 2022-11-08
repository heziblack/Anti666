package org.hezistudio

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.globalEventChannel
import net.mamoe.mirai.message.data.MessageChainBuilder
import net.mamoe.mirai.message.data.MessageSource.Key.recall
import net.mamoe.mirai.message.data.QuoteReply
import net.mamoe.mirai.message.data.content
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import net.mamoe.mirai.utils.info
import okhttp3.Request
import java.io.File


object Anti666 : KotlinPlugin(
    JvmPluginDescription(
        id = "org.hezistudio.anti666",
        name = "怼666",
        version = "0.1.1",
    ) {
        author("HeziBlack")
    }
) {
    private var ownerID:Long = -1L
    private var mode:AntiMode = AntiMode.Stop
    private var listOfReplaceNum = listOf<Int>(0,1,2,3,4,5,7,8,9)
    private var kkwSwitch = false
    private var mmwSwitch = false
    override fun onEnable() {
        ownerID = readOwner()
        logger.info("插件主人：${ownerID} 默认模式：${mode.cnName}")
        val groupMessageChannel = globalEventChannel().filter {
            it is GroupMessageEvent
        }

        //**反666*/
        groupMessageChannel.filter {
            val r = Regex("""6+""")
            r.matches((it as GroupMessageEvent).message.content )
        }.subscribeAlways<GroupMessageEvent> {
            when(mode){
                AntiMode.Text -> {
                    val qr = QuoteReply(message)
                    val n = listOfReplaceNum.random()
                    val msg = message.content.replace("6","$n")
                    val mb = MessageChainBuilder()
                    mb.add(qr)
                    mb.add(msg)
                    group.sendMessage(mb.build())
                }
                AntiMode.Image -> {
                    val mb = MessageChainBuilder()
                    val qr = QuoteReply(message)
                    val imgRes = getResourceAsStream("antiImage.jpg")
                    if (imgRes==null){
                        logger.warning("没有找到图片资源")
                    }else{
                        val external = imgRes.use { it.toExternalResource() }
                        external.use {
                            val img = group.uploadImage(external)
                            mb.add(qr)
                            mb.add(img)
                        }
                        withContext(Dispatchers.IO) {
                            external.close()
                            imgRes.close()
                        }
                        group.sendMessage(mb.build())
                    }
                }
                AntiMode.Recall -> {
                    if (group.botPermission>sender.permission){
                        message.recall()
                    }else{
                        val qr = QuoteReply(message)
                        val n = listOfReplaceNum.random()
                        val msg = message.content.replace("6","$n")
                        val mb = MessageChainBuilder()
                        mb.add(qr)
                        mb.add(msg)
                        group.sendMessage(mb.build())
                        delay(500)
                        group.sendMessage("权限不足,无法撤回该消息")
                    }
                }
                else -> {}
            }
        }

        //**反666模式切换指令*/
        groupMessageChannel.filter {
            val gme = it as GroupMessageEvent
            val r = Regex("""#anti6 [0123]""")
            val cmd = r.matches(gme.message.content)
            val owner = (gme.sender.id) == ownerID
            cmd && owner
        }.subscribeAlways<GroupMessageEvent> {
            mode = when(message.content.last()){
                '0'->{
                    AntiMode.Stop
                }
                '1'->{
                    AntiMode.Text
                }
                '2'->{
                    AntiMode.Image
                }
                '3'->{
                    AntiMode.Recall
                }
                else-> AntiMode.Stop
            }
            group.sendMessage("模式修改成功！(${mode.cnName})")
        }

        // 夸夸我
        groupMessageChannel.filter { (it as GroupMessageEvent).message.content == "夸夸我" && kkwSwitch}.subscribeAlways<GroupMessageEvent> {
            val client = okhttp3.OkHttpClient()
            val r = Request.Builder().url("https://api.shadiao.pro/chp").build()
            val a = client.newCall(r).execute()
            if (a.code == 200){
                val json = a.body!!.charStream().use {
                    it.readText()
                }

                val js = json.replace(Regex("""\\u[0-9a-f]{4}""")) { matchResult ->
                    val ucode = matchResult.value.substring(2)
                    ucode.toInt(16).toChar().toString()
                }
//                logger.info(json)
//                logger.info(js)
                val jo = Gson().fromJson(js,Chp::class.java)
                group.sendMessage(jo.data.text)
            }
        }

        // 夸夸我开关
        groupMessageChannel.filter {
            val gme = it as GroupMessageEvent
            val r = Regex("""#kkw""")
            val cmd = r.matches(gme.message.content)
            val owner = (gme.sender.id) == ownerID
            cmd && owner
        }.subscribeAlways<GroupMessageEvent> {
            kkwSwitch = !kkwSwitch
            if (kkwSwitch){
                group.sendMessage("夸夸我功能已开启")
            }else{
                group.sendMessage("夸夸我功能已关闭")
            }
        }

        // 骂骂我
        groupMessageChannel.filter {
            val e = it as GroupMessageEvent
            val cmd = e.message.content == "骂骂我"
            cmd && mmwSwitch
        }.subscribeAlways<GroupMessageEvent> {
            val client = okhttp3.OkHttpClient()
            val params = listOf<String>("min", "max")
            val r = Request.Builder().url("https://nmsl.yfchat.xyz/api.php?level=${params.random()}").build()
            val a = client.newCall(r).execute()
            if (a.code == 200){
                val result = a.body?.string()?:return@subscribeAlways
                group.sendMessage(result)
            }
        }

        // 骂骂我开关
        groupMessageChannel.filter {
            val gme = it as GroupMessageEvent
            val r = Regex("""#mmw""")
            val cmd = r.matches(gme.message.content)
            val owner = (gme.sender.id) == ownerID
            cmd && owner
        }.subscribeAlways<GroupMessageEvent> {
            mmwSwitch = !mmwSwitch
            if (mmwSwitch){
                group.sendMessage("骂骂我功能已开启，请友善使用")
            }else{
                group.sendMessage("骂骂我功能已关闭")
            }
        }

        logger.info { "Plugin loaded" }
    }

    enum class AntiMode(val cnName:String){
        Stop("关闭"),
        Text("文字模式"),
        Image("图片模式"),
        Recall("撤回模式")
    }

    class Chp(val data:ChpData)

    class ChpData(val type:String,val text:String)

    private fun readOwner():Long{
        val f = File(dataFolder,"data.txt")
        if (!f.exists()){
            if (!dataFolder.exists()){
                dataFolder.mkdirs()
            }
            f.createNewFile()
            f.writeText("-1")
        }
        return try {
            val lines = f.readLines()
            lines[0].toLong()
        } catch (e: Exception) {
            -1L
        }
    }

}