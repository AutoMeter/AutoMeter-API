#!/bin/sh
 function getIpAddr() 
 {
        # 获取IP命令
        echo "开始获取ip"
        ipaddr=`ifconfig -a|grep inet|grep -v 127.0.0.1|grep -v inet6|awk '{print $2}'|tr -d "addr:"`
        echo "获取ip：$ipaddr"
        array=(`echo $ipaddr | tr '\n' ' '` )  # IP地址分割，区分是否多网卡
        #array=(172.20.32.214 192.168.1.10 192.168.1.2 192.168.1.10 192.168.1.2 192.168.1.10 192.168.1.2 192.168.1.10 192.168.1.2 192.168.1.10 192.168.1.2);
        num=${#array[@]}                                                #获取数组元素的个数
        echo "ip数组长度为$num"

        # 选择安装的IP地址
        if [ $num -eq 1 ]; then
                echo "*单网卡"
                local_ip=${array[*]}
        elif [ $num -gt 1 ];then
                echo -e "\033[035m******************************\033[0m"
                echo -e "\033[036m*    请选择安装的IP地址               \033[0m"
                for ((i=0; i<=$num-1;i++))
                do
                        #echo $num
                        echo -e "\033[032m*     $i : ${array[$i]}                \033[0m"
                done    
                #echo -e "\033[032m*      1 : ${array[0]}               \033[0m"
                #echo -e "\033[034m*      2 : ${array[1]}               \033[0m"
                echo -e "\033[035m******************************\033[0m"
                #选择需要安装的服务类型
                input=""
                while :
                do
                        read -r -p "*请选择安装的IP地址(序号): " input
                        case $input in
                           [0-9]*)
                                        local_ip=${array[$input]}
                                        #echo "选择网段1的IP为：${local_ip}"
                                        break
                                        ;;
                                *)
                                echo "*请输入有效的数字:"
                                        ;;
                        esac
                done
        else
                echo -e "\033[31m*未设置网卡IP，请检查服务器环境！ \033[0m"
                exit 1
        fi
} 

# 校验IP地址合法性
function isValidIp() 
{
        local ip=$1
        local ret=1
 
        if [[ $ip =~ ^[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}$ ]]; then
                ip=(${ip//\./ }) # 按.分割，转成数组，方便下面的判断
                [[ ${ip[0]} -le 255 && ${ip[1]} -le 255 && ${ip[2]} -le 255 && ${ip[3]} -le 255 ]]
                ret=$?
        fi
        return $ret
}

CURRENT_DIR=$(cd $(dirname $0); pwd)

# 初始化数据库
# echo "开始数据库操作。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。"

# mysql -u root -p 123456 -e "create database if NOT EXISTS testcenter default character set utf8mb4 collate utf8mb4_bin"

# echo "创建数据库testcenter完成。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。"


# initsql=/sql/init-sql/*.sql
# initsqlfile=$CURRENT_DIR$initsql

# for i in $(find $initsqlfile) ; do
#   mysql -u root -p 123456 testcenter < ${i};
# done

# echo "初始化数据库testcenter完成。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。"


# updatesql=/sql/update-sql/*.sql
# updatesqlfile=$CURRENT_DIR$updatesql

# # 更新sql
# for i in $(find $updatesqlfile) ; do
#   mysql -u root -p 123456 testcenter < ${i};
# done

# echo "更新数据库testcenter完成。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。"

# echo "数据库操作完成。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。"

 
local_ip=''
echo "开始配置AutoMeter后端服务IP。。。。。。。"
getIpAddr       #自动获取IP
isValidIp ${local_ip}   # IP校验
if [ $? -ne 0 ]; then
        echo -e "\033[31m*自动获取的IP地址无效，请重试！ \033[0m"
        exit 1
fi
echo "*选择安装AutoMeter服务的IP地址为：${local_ip}"

os=`uname  -os`
b="Darwin"
c="centos"
d="ubuntu"

if [[ $os =~ $b ]];then
    echo "mac"
    sed -i "" "s@127.0.0.1@${local_ip}@" ../AutoMeter/conditionservice/config/application.yml
    sed -i "" "s@127.0.0.1@${local_ip}@" ../AutoMeter/dispatchservice/config/application.yml 
    sed -i "" "s@127.0.0.1@${local_ip}@" ../AutoMeter/slaverservice/config/application.yml 
    sed -i "" "s@127.0.0.1@${local_ip}@" ../AutoMeter/mockservice/config/application.yml 
    sed -i "" "s@127.0.0.1@${local_ip}@" ../AutoMeter/testcenterservice/config/application.yml 
    sed -i "" "s@127.0.0.1@${local_ip}@" ../AutoMeter/testcenterapp/dist/static/config.js
else
    echo $os
    sed -i  "s@127.0.0.1@${local_ip}@" ../AutoMeter/conditionservice/config/application.yml
    sed -i  "s@127.0.0.1@${local_ip}@" ../AutoMeter/dispatchservice/config/application.yml 
    sed -i  "s@127.0.0.1@${local_ip}@" ../AutoMeter/mockservice/config/application.yml 
    sed -i  "s@127.0.0.1@${local_ip}@" ../AutoMeter/slaverservice/config/application.yml 
    sed -i  "s@127.0.0.1@${local_ip}@" ../AutoMeter/testcenterservice/config/application.yml 
    sed -i  "s@127.0.0.1@${local_ip}@" ../AutoMeter/testcenterapp/dist/static/config.js
fi
#sed -i  "s@127.0.0.1@${local_ip}@" ../conditionservice/config/application.yml
#sed -i  "s@127.0.0.1@${local_ip}@" ../dispatchservice/config/application.yml 
#sed -i  "s@127.0.0.1@${local_ip}@" ../slaverservice/config/application.yml 
#sed -i  "s@127.0.0.1@${local_ip}@" ../testcenterservice/config/application.yml 
#sed -i  "s@127.0.0.1@${local_ip}@" ../testcenterapp/dist/static/config.js
echo "配置AutoMeter后端服务IP成功。。。。。。。"

 
conditionservicejar=../AutoMeter/conditionservice/conditionservice.jar
conditionserviceyml=../AutoMeter/conditionservice/config/application.yml

echo "AutoMeter-conditionservice开始启动。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。"

nohup java -Xms256m -Xmx256m -Xmn128m -XX:ReservedCodeCacheSize=120M -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=256m -XX:+UseConcMarkSweepGC -XX:MaxTenuringThreshold=6 -XX:+ExplicitGCInvokesConcurrent -XX:-UseBiasedLocking -XX:-UseCounterDecay -XX:AutoBoxCacheMax=20000 -XX:+PerfDisableSharedMem -XX:+AlwaysPreTouch -Djava.security.egd=file:/dev/./urandom  -jar $CURRENT_DIR$conditionservicejar --spring.config.location=$CURRENT_DIR$conditionserviceyml &

sleep 3
echo "AutoMeter-conditionservice启动成功"

mockservicejar=../AutoMeter/mockservice/mockservice.jar
mockserviceyml=../AutoMeter/mockservice/config/application.yml

echo "AutoMeter-mockservicejar开始启动。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。"

nohup java -Xms256m -Xmx256m -Xmn128m -XX:ReservedCodeCacheSize=120M -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=256m -XX:+UseConcMarkSweepGC -XX:MaxTenuringThreshold=6 -XX:+ExplicitGCInvokesConcurrent -XX:-UseBiasedLocking -XX:-UseCounterDecay -XX:AutoBoxCacheMax=20000 -XX:+PerfDisableSharedMem -XX:+AlwaysPreTouch -Djava.security.egd=file:/dev/./urandom  -jar $CURRENT_DIR$mockservicejar --spring.config.location=$CURRENT_DIR$mockserviceyml &

sleep 3
echo "AutoMeter-conditionservice启动成功"

slaverservicejar=../AutoMeter/slaverservice/slaverservice.jar
slaverserviceserviceyml=../AutoMeter/slaverservice/config/application.yml

echo "AutoMeter-slaverservice开始启动。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。"

nohup java -Xms256m -Xmx256m -Xmn128m -XX:ReservedCodeCacheSize=120M -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=256m -XX:+UseConcMarkSweepGC -XX:MaxTenuringThreshold=6 -XX:+ExplicitGCInvokesConcurrent -XX:-UseBiasedLocking -XX:-UseCounterDecay -XX:AutoBoxCacheMax=20000 -XX:+PerfDisableSharedMem -XX:+AlwaysPreTouch -Djava.security.egd=file:/dev/./urandom  -jar $CURRENT_DIR$slaverservicejar --spring.config.location=$CURRENT_DIR$slaverserviceserviceyml &

sleep 3
echo "AutoMeter-slaverservice启动成功"

testcenterservicejar=../AutoMeter/testcenterservice/testcenterservice.jar
testcenterserviceserviceyml=../AutoMeter/testcenterservice/config/application.yml

echo "AutoMeter-testcenterservice开始启动。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。"

nohup java -Xms256m -Xmx256m -Xmn128m -XX:ReservedCodeCacheSize=120M -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=256m -XX:+UseConcMarkSweepGC -XX:MaxTenuringThreshold=6 -XX:+ExplicitGCInvokesConcurrent -XX:-UseBiasedLocking -XX:-UseCounterDecay -XX:AutoBoxCacheMax=20000 -XX:+PerfDisableSharedMem -XX:+AlwaysPreTouch -Djava.security.egd=file:/dev/./urandom  -jar $CURRENT_DIR$testcenterservicejar --spring.config.location=$CURRENT_DIR$testcenterserviceserviceyml &
sleep 3
echo "AutoMeter-testcenterservice启动成功"

dispatchservicejar=../AutoMeter/dispatchservice/dispatchservice.jar
dispatchserviceserviceyml=../AutoMeter/dispatchservice/config/application.yml

echo "AutoMeter-dispatchservice开始启动。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。"

nohup java -Xms256m -Xmx256m -Xmn128m -XX:ReservedCodeCacheSize=120M -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=256m -XX:+UseConcMarkSweepGC -XX:MaxTenuringThreshold=6 -XX:+ExplicitGCInvokesConcurrent -XX:-UseBiasedLocking -XX:-UseCounterDecay -XX:AutoBoxCacheMax=20000 -XX:+PerfDisableSharedMem -XX:+AlwaysPreTouch -Djava.security.egd=file:/dev/./urandom  -jar $CURRENT_DIR$dispatchservicejar --spring.config.location=$CURRENT_DIR$dispatchserviceserviceyml &
sleep 3
echo "AutoMeter-dispatchservice启动成功"


echo "AutoMeter后台服务部署成功，配置nginx.conf中的http.server.location.root配置testcenterapp所在的目录，例如：/app/AutoMeter/testcenterapp/dist/，访问入口 http://$local_ip:nginx端口  默认账户密码admin admin123"






