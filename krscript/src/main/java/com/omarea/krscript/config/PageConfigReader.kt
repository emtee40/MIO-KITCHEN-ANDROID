package com.omarea.krscript.config

import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Xml
import android.widget.Toast
import com.omarea.krscript.executor.ExtractAssets
import com.omarea.krscript.executor.ScriptEnvironmen
import com.omarea.krscript.model.*
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.util.*
import kotlin.collections.ArrayList

/**
 * Created by Hello on 2018/04/01.
 */
class PageConfigReader(private var context: Context) {
    private val ASSETS_FILE = "file:///android_asset/"
    private fun getConfig(context: Context, filePath: String): InputStream? {
        try {
            if (filePath.startsWith(ASSETS_FILE)) {
                return context.assets.open(filePath.substring(ASSETS_FILE.length))
            } else {
                return context.assets.open(filePath)
            }
        } catch (ex: Exception) {
            return null
        }
    }

    fun readConfigXml(filePath: String): ArrayList<ConfigItemBase>? {
        try {
            val fileInputStream = getConfig(context, filePath) ?: return ArrayList()
            val parser = Xml.newPullParser()// 获取xml解析器
            parser.setInput(fileInputStream, "utf-8")// 参数分别为输入流和字符编码
            var type = parser.eventType
            val mainList: ArrayList<ConfigItemBase> = ArrayList()
            var action: ActionInfo? = null
            var switch: SwitchInfo? = null
            var picker: PickerInfo? = null
            var group: GroupInfo? = null
            var page: PageInfo? = null
            var text: TextInfo? = null
            while (type != XmlPullParser.END_DOCUMENT) {// 如果事件不等于文档结束事件就继续循环
                when (type) {
                    XmlPullParser.START_TAG ->
                        if ("group" == parser.name) {
                            group = groupNode(GroupInfo(), parser)
                        }
                        else if (group != null && !group.supported) {
                            // 如果 group.supported !- true 跳过group内所有项
                        }
                        else {
                            if ("page" == parser.name) {
                                page = mainNode(PageInfo(), parser) as PageInfo?
                                if (page != null) {
                                    page = pageNode(page, parser)
                                }
                            } else if ("action" == parser.name) {
                                action = mainNode(ActionInfo(), parser) as ActionInfo?
                            } else if ("switch" == parser.name) {
                                switch = mainNode(SwitchInfo(), parser) as SwitchInfo?
                            } else if ("picker" == parser.name) {
                                picker = mainNode(PickerInfo(), parser) as PickerInfo?
                            } else if ("text" == parser.name) {
                                text = mainNode(TextInfo(), parser) as TextInfo?
                            } else if (page != null) {
                                tagStartInPage(page, parser)
                            } else if (action != null) {
                                tagStartInAction(action, parser)
                            } else if (switch != null) {
                                tagStartInSwitch(switch, parser)
                            } else if (picker != null) {
                                tagStartInPicker(picker, parser)
                            } else if (text != null) {
                                tagStartInText(text, parser)
                            } else if ("resource" == parser.name) {
                                resourceNode(parser)
                            }
                        }
                    XmlPullParser.END_TAG ->
                        if ("group" == parser.name) {
                            if (group != null) {
                                mainList.add(group)
                            }
                            group = null
                        } else if (group != null) {
                            if ("page" == parser.name) {
                                tagEndInPage(page, parser)
                                if (page != null) {
                                    group.children.add(page)
                                }
                                page = null
                            }
                            else if ("action" == parser.name) {
                                tagEndInAction(action, parser)
                                if (action != null) {
                                    group.children.add(action)
                                }
                                action = null
                            }
                            else if ("switch" == parser.name) {
                                tagEndInSwitch(switch, parser)
                                if (switch != null) {
                                    group.children.add(switch)
                                }
                                switch = null
                            }
                            else if ("picker" == parser.name) {
                                tagEndInPicker(picker, parser)
                                if (picker != null) {
                                    group.children.add(picker)
                                }
                                picker = null
                            }
                            else if ("text" == parser.name) {
                                tagEndInText(text, parser)
                                if (text != null) {
                                    group.children.add(text)
                                }
                                text = null
                            }
                        } else {
                            if ("page" == parser.name) {
                                tagEndInPage(page, parser)
                                if (page != null) {
                                    mainList.add(page)
                                }
                                page = null
                            }
                            else if ("action" == parser.name) {
                                tagEndInAction(action, parser)
                                if (action != null) {
                                    mainList.add(action)
                                }
                                action = null
                            }
                            else if ("switch" == parser.name) {
                                tagEndInSwitch(switch, parser)
                                if (switch != null) {
                                    mainList.add(switch)
                                }
                                switch = null
                            }
                            else if ("picker" == parser.name) {
                                tagEndInPicker(picker, parser)
                                if (picker != null) {
                                    mainList.add(picker)
                                }
                                picker = null
                            }
                            else if ("text" == parser.name) {
                                tagEndInText(text, parser)
                                if (text != null) {
                                    mainList.add(text)
                                }
                                text = null
                            }
                        }
                }
                type = parser.next()// 继续下一个事件
            }

            return mainList
        } catch (ex: Exception) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "解析配置文件失败\n" + ex.message, Toast.LENGTH_LONG).show()
            }
            Log.e("KrConfig Fail！", "" + ex.message)
        }

        return null
    }

    var actionParamInfos: ArrayList<ActionParamInfo>? = null
    var actionParamInfo: ActionParamInfo? = null
    private fun tagStartInAction(action: ActionInfo, parser:XmlPullParser) {
        if ("title" == parser.name) {
            action.title = parser.nextText()
        }
        else if ("desc" == parser.name) {
            descNode(action, parser)
        }
        else if ("script" == parser.name) {
            action.script = parser.nextText().trim()
        }
        else if ("param" == parser.name) {
            if (actionParamInfos == null) {
                actionParamInfos = ArrayList()
            }
            actionParamInfo = ActionParamInfo()
            val actionParamInfo = actionParamInfo!!
            for (i in 0 until parser.attributeCount) {
                val attrName = parser.getAttributeName(i)
                val attrValue = parser.getAttributeValue(i)
                when {
                    attrName == "name" -> actionParamInfo.name = attrValue
                    attrName == "label" -> actionParamInfo.label = attrValue
                    attrName == "title" -> actionParamInfo.title = attrValue
                    attrName == "desc" -> actionParamInfo.desc = attrValue
                    attrName == "value" -> actionParamInfo.value = attrValue
                    attrName == "type" -> actionParamInfo.type = attrValue.toLowerCase().trim { it <= ' ' }
                    attrName == "readonly" -> {
                        val value = attrValue.toLowerCase().trim { it <= ' ' }
                        actionParamInfo.readonly =  value == "readonly" || value == "true" || value == "1"
                    }
                    attrName == "maxlength" -> actionParamInfo.maxLength = Integer.parseInt(attrValue)
                    attrName == "min" -> actionParamInfo.min = Integer.parseInt(attrValue)
                    attrName == "max" -> actionParamInfo.max = Integer.parseInt(attrValue)
                    attrName == "required" -> actionParamInfo.required = attrValue == "true" || attrValue == "1" || attrValue == "required"
                    attrName == "value-sh" || attrName == "value-su" -> {
                        val script = attrValue
                        actionParamInfo.valueShell = script
                    }
                    attrName == "options-sh" || attrName == "options-su" -> {
                        if (actionParamInfo.options == null)
                            actionParamInfo.options = ArrayList<ActionParamInfo.ActionParamOption>()
                        val script = attrValue
                        actionParamInfo.optionsSh = script
                    }
                    attrName == "support" -> {
                        if (executeResultRoot(context, attrValue) != "1") {
                            actionParamInfo.supported = false
                        }
                    }
                }
            }
            if (actionParamInfo.supported && actionParamInfo.name != null && actionParamInfo.name!!.isNotEmpty()) {
                actionParamInfos!!.add(actionParamInfo)
            }
        }
        else if (actionParamInfo != null && "option" == parser.name) {
            val actionParamInfo = actionParamInfo!!
            if (actionParamInfo.options == null) {
                actionParamInfo.options = ArrayList()
            }
            val option = ActionParamInfo.ActionParamOption()
            for (i in 0 until parser.attributeCount) {
                val attrName = parser.getAttributeName(i)
                if (attrName == "val" || attrName == "value") {
                    option.value = parser.getAttributeValue(i)
                }
            }
            option.desc = parser.nextText()
            if (option.value == null)
                option.value = option.desc
            actionParamInfo.options!!.add(option)
        }
        else if ("resource" == parser.name) {
            resourceNode(parser)
        }
    }


    private fun tagEndInPage(page: PageInfo?, parser: XmlPullParser) {
        if (page != null) {
            if (page.id.isEmpty() && page.title.isNotEmpty()) {
                page.id = page.title
            }

            actionParamInfos = null
        }
    }

    private fun tagEndInAction(action: ActionInfo?, parser:XmlPullParser) {
        if (action != null) {
            if (action.script == null)
                action.script = ""
            action.params = actionParamInfos
            if (action.id.isEmpty() && action.title.isNotEmpty()) {
                action.id = action.title
            }

            actionParamInfos = null
        }
    }

    private fun tagStartInPage(info: PageInfo, parser: XmlPullParser) {
        when {
            "title" == parser.name -> info.title = parser.nextText()
            "desc" == parser.name -> descNode(info, parser)
            "resource" == parser.name -> resourceNode(parser)
        }
    }

    private fun tagStartInSwitch(switchInfo: SwitchInfo, parser:XmlPullParser) {
        when {
            "title" == parser.name -> switchInfo.title = parser.nextText()
            "desc" == parser.name -> descNode(switchInfo, parser)
            "get" == parser.name || "getstate" == parser.name -> switchInfo.getState = parser.nextText()
            "set" == parser.name || "setstate" == parser.name -> switchInfo.setState = parser.nextText()
            "resource" == parser.name -> resourceNode(parser)
        }
    }

    private fun groupNode(groupInfo: GroupInfo, parser: XmlPullParser): GroupInfo {
        val groupInfo = GroupInfo()
        for (i in 0 until parser.attributeCount) {
            val attrName = parser.getAttributeName(i)
            val attrValue = parser.getAttributeValue(i)
            if (attrName == "title") {
                groupInfo.separator = attrValue
            } else if (attrName == "support") {
                groupInfo.supported = executeResultRoot(context, attrValue) == "1"
            }
        }
        return groupInfo
    }

    private fun mainNode(configItemBase: ConfigItemBase, parser: XmlPullParser): ConfigItemBase? {
        for (i in 0 until parser.attributeCount) {
            val attrValue = parser.getAttributeValue(i)
            when (parser.getAttributeName(i)) {
                "id" -> configItemBase.id = attrValue
                "confirm" -> configItemBase.confirm = attrValue == "true"
                "auto-off" -> configItemBase.autoOff = attrValue == "true"
                "interruptible" -> configItemBase.interruptible = attrValue != "false"
                "support" -> {
                    if (executeResultRoot(context, attrValue) != "1") {
                        return null
                    }
                }
                /* "options-sh", "options-su" -> {
                        if (configItemBase.options == null)
                            configItemBase.options = ArrayList()
                        configItemBase.optionsSh = attrValue
                    }
                */
            }
        }
        if (configItemBase.id.isEmpty()) {
            configItemBase.id = UUID.randomUUID().toString()
        }
        return configItemBase
    }


    private fun pageNode(page: PageInfo, parser: XmlPullParser): PageInfo {
        for (attrIndex in 0 until parser.attributeCount) {
            val attrName = parser.getAttributeName(attrIndex)
            if (attrName == "config") {
                val value = parser.getAttributeValue(attrIndex)
                page.pageConfigPath = value
            } else if (attrName == "html") {
                val value = parser.getAttributeValue(attrIndex)
                page.onlineHtmlPage = value
            } else if (attrName == "title") {
                val value = parser.getAttributeValue(attrIndex)
                page.title = value
            } else if (attrName == "desc") {
                val value = parser.getAttributeValue(attrIndex)
                page.desc = value
            }
        }
        return page
    }

    private fun descNode(configItemBase: ConfigItemBase, parser: XmlPullParser) {
        for (i in 0 until parser.attributeCount) {
            val attrName = parser.getAttributeName(i)
            if (attrName == "su" || attrName == "sh") {
                configItemBase.descPollingShell = parser.getAttributeValue(i)
                configItemBase.desc = executeResultRoot(context, configItemBase.descPollingShell)
            }
        }
        if (configItemBase.desc.isEmpty())
            configItemBase.desc = parser.nextText()
    }

    private fun resourceNode(parser: XmlPullParser) {
        for (i in 0 until parser.attributeCount) {
            if (parser.getAttributeName(i) == "file") {
                val file = parser.getAttributeValue(i).trim()
                if (file.startsWith(ASSETS_FILE)) {
                    ExtractAssets(context).extractResource(file)
                }
            } else if (parser.getAttributeName(i) == "dir") {
                val file = parser.getAttributeValue(i).trim()
                if (file.startsWith(ASSETS_FILE)) {
                    ExtractAssets(context).extractResources(file)
                }
            }
        }
    }

    private fun tagEndInSwitch(switchInfo: SwitchInfo?, parser:XmlPullParser) {
        if (switchInfo != null) {
            if (switchInfo.getState == null) {
                switchInfo.getState = ""
            } else {
                val shellResult = executeResultRoot(context, switchInfo.getState)
                switchInfo.checked = shellResult != "error" && (shellResult == "1" || shellResult.toLowerCase() == "true")
            }
            if (switchInfo.setState == null) {
                switchInfo.setState = ""
            }
            if (switchInfo.id.isEmpty() && switchInfo.title.isNotEmpty()) {
                switchInfo.id = switchInfo.title
            }
        }
    }

    private fun tagStartInText(textInfo: TextInfo, parser: XmlPullParser) {
        if ("title" == parser.name) {
            textInfo.title = parser.nextText()
        }
        else if ("desc" == parser.name) {
            descNode(textInfo, parser)
        }
        else if ("row" == parser.name) {
            rowNode(textInfo, parser)
        }
    }

    private fun rowNode(textInfo: TextInfo, parser: XmlPullParser) {
        try {
            val count = parser.attributeCount
            for (i in 0 until count) {
                val attrName = parser.getAttributeName(i)
                val attrValue = parser.getAttributeValue(i)
                val textRow = TextInfo.TextRow()
                try {
                    when (attrName) {
                        "bold" -> textRow.bold = (attrValue == "1" || attrValue == "true" || attrValue == "bold")
                        "italic" -> textRow.italic = (attrValue == "1" || attrValue == "true" || attrValue == "italic")
                        "color" -> textRow.color = Color.parseColor(attrValue)
                        "size" -> textRow.size = attrValue.toInt()
                        "break" -> textRow.breakRow = (attrValue == "1" || attrValue == "true" || attrValue == "break")
                    }
                } catch (ex: Exception) {
                }
                try {
                    textRow.text = parser.nextText()
                    textInfo.rows.add(textRow)
                } catch (ex: java.lang.Exception) {
                    Log.e("KrConfig rowNode", "" + ex.message)
                }
            }
        } catch (ex: java.lang.Exception) {
            Log.e("KrConfig rowNode", "" + ex.message)
        }
    }

    private fun tagStartInPicker(pickerInfo: PickerInfo, parser:XmlPullParser) {
        if ("title" == parser.name) {
            pickerInfo.title = parser.nextText()
        }
        else if ("desc" == parser.name) {
            descNode(pickerInfo, parser)
        }
        else if ("option" == parser.name) {
            if (pickerInfo.options === null) {
                pickerInfo.options = ArrayList()
            }
            val option = ActionParamInfo.ActionParamOption()
            for (i in 0 until parser.attributeCount) {
                val attrName = parser.getAttributeName(i)
                if (attrName == "val" || attrName == "value") {
                    option.value = parser.getAttributeValue(i)
                }
            }
            option.desc = parser.nextText()
            if (option.value == null)
                option.value = option.desc
            pickerInfo.options!!.add(option)
        }
        else if ("getstate" == parser.name || "get" == parser.name) {
            pickerInfo.getState = parser.nextText()
        }
        else if ("setstate" == parser.name || "set" == parser.name) {
            pickerInfo.setState = parser.nextText()
        }
    }

    private fun tagEndInPicker(pickerInfo: PickerInfo?, parser:XmlPullParser) {
        if (pickerInfo != null) {
            if (pickerInfo.getState == null) {
                pickerInfo.getState = ""
            } else {
                val shellResult = executeResultRoot(context, "" + pickerInfo.getState)
                pickerInfo.value = shellResult
            }
            if (pickerInfo.setState == null) {
                pickerInfo.setState = ""
            }
            if (pickerInfo.id.isEmpty() && pickerInfo.title.isNotEmpty()) {
                pickerInfo.id = pickerInfo.title
            }
        }
    }

    private fun tagEndInText(textInfo: TextInfo?, parser: XmlPullParser) {
        if (textInfo != null) {
            if (textInfo.id.isEmpty() && textInfo.title.isNotEmpty()) {
                textInfo.id = textInfo.title
            }
        }
    }

    private fun executeResultRoot(context: Context, scriptIn: String): String {
        return ScriptEnvironmen.executeResultRoot(context, scriptIn);
    }
}