package cn.hzq.chatgml.infrastructure.util.sdk;

import cn.hutool.core.util.StrUtil;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.core.util.QuickWriter;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.xml.PrettyPrintWriter;
import com.thoughtworks.xstream.io.xml.XppDriver;
import com.thoughtworks.xstream.mapper.MapperWrapper;

import java.io.Writer;

/**
 * @author 黄照权
 * @Date 2024/4/13
 * @Description 微信公众号发送消息，解析工具类
 **/
public class XmlUtil {
    private static final XStream xstream = initXStream();

    /**
     * 反序列化 XML 为 Bean
     *
     * @param xml   XML
     * @param clazz 目标 Bean 类型
     * @return Bean
     */
    public static <T> T xmlToBean(String xml, Class<T> clazz) {
        xstream.alias("xml", clazz);
        xstream.allowTypes(new Class[]{clazz});
        return clazz.cast(xstream.fromXML(xml));
    }

    /**
     * 序列化 Bean 为 XML
     *
     * @param bean Bean
     * @return XML
     */
    public static String beanToXml(Object bean) {
        xstream.alias("xml", bean.getClass());
        return xstream.toXML(bean);
    }

    /**
     * 初始化并配置 XStream 实例
     */
    private static XStream initXStream() {
        return new XStream(new XppDriver() {
            @Override
            public HierarchicalStreamWriter createWriter(Writer out) {
                return new PrettyPrintWriter(out) {
                    final boolean cdataWrap = true;
                    final String cdataFormat = "<![CDATA[%s]]>";

                    /**
                     * 序列化为 XML 时将节点的首字母大写
                     */
                    @Override
                    public void startNode(String name, Class clazz) {
                        if ("xml".equals(name)) {
                            super.startNode(name, clazz);
                        } else {
                            super.startNode(StrUtil.upperFirst(name), clazz);
                        }
                    }

                    /**
                     * 使用 CDATA 包裹文本节点
                     */
                    @Override
                    protected void writeText(QuickWriter writer, String text) {
                        if (cdataWrap && !StrUtil.isNumeric(text)) {
                            writer.write(String.format(cdataFormat, text));
                        } else {
                            writer.write(text);
                        }
                    }
                };
            }
        }) {
            /**
             * 反序列化为 Bean 时将节点的首字母小写
             */
            @Override
            protected MapperWrapper wrapMapper(MapperWrapper next) {
                return new MapperWrapper(next) {
                    @Override
                    public String realMember(Class type, String serialized) {
                        return super.realMember(type, StrUtil.lowerFirst(serialized));
                    }
                };
            }
        };
    }

}
