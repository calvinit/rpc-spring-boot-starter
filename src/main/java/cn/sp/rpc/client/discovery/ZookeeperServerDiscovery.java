package cn.sp.rpc.client.discovery;

import cn.sp.rpc.common.constants.RpcConstant;
import cn.sp.rpc.common.model.Service;
import cn.sp.rpc.common.serializer.ZookeeperSerializer;
import com.alibaba.fastjson.JSON;
import org.I0Itec.zkclient.ZkClient;
import org.springframework.beans.factory.InitializingBean;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author 2YSP
 * @date 2020/7/25 19:49
 */
public class ZookeeperServerDiscovery implements ServerDiscovery, InitializingBean {

    private ZkClient zkClient;

    static final String ZK_BASE_PATH = RpcConstant.ZK_SERVICE_PATH + RpcConstant.PATH_DELIMITER;

    public ZookeeperServerDiscovery(String zkAddress) {
        zkClient = new ZkClient(zkAddress);
        zkClient.setZkSerializer(new ZookeeperSerializer());
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        ServerDiscoveryCache.SERVICE_CLASS_NAMES.forEach(name ->{
            String servicePath = ZK_BASE_PATH + name + "/service";
            zkClient.subscribeChildChanges(servicePath, new ZkChildListenerImpl());
        });
    }

    /**
     * 使用Zookeeper客户端，通过服务名获取服务列表
     * 服务名格式：接口全路径
     *
     * @param name
     * @return
     */
    @Override
    public List<Service> findServiceList(String name) {
        String servicePath = ZK_BASE_PATH + name + "/service";
        List<String> children = zkClient.getChildren(servicePath);
        return Optional.ofNullable(children).orElse(new ArrayList<>()).stream().map(str -> {
            String deCh = null;
            try {
                deCh = URLDecoder.decode(str, RpcConstant.UTF_8);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            return JSON.parseObject(deCh, Service.class);
        }).collect(Collectors.toList());
    }
}
