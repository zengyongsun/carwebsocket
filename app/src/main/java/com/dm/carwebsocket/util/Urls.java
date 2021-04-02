package com.dm.carwebsocket.util;


/**
 * @author : Zeyo
 * e-mail : zengyongsun@163.com
 * date   : 2019/9/26 10:08
 * desc   :
 * version: 1.0
 */
public interface Urls {

    String host_ip = "http://" + LocalArguments.getInstance().serviceIp() + ":"
            + LocalArguments.getInstance().servicePort();

    /**
     * {
     * "update": "Yes",
     * "new_version": "0.8.3",
     * "apk_file_url": "https://raw.githubusercontent.com/WVector/AppUpdateDemo/master/apk/sample-debug.apk",
     * "update_log": "更新日志",
     * "target_size": "5M",
     * "new_md5":"b97bea014531123f94c3ba7b7afbaad2",
     * "constraint": false
     * }
     */
    String app_update_url = "/SmartMine/TerminalManage/Terminal/GetVersionUpdate";


}
