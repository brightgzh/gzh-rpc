package com.gzh.remoting.transport.netty.codec;


import com.gzh.compress.Compress;
import com.gzh.enums.CompressTypeEnum;
import com.gzh.enums.SerializationTypeEnum;
import com.gzh.extension.ExtensionLoader;
import com.gzh.remoting.constants.RpcConstants;
import com.gzh.remoting.dto.RpcMessage;
import com.gzh.serialize.Serializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicInteger;


/**
 * <p>
 * custom protocol decoder
 * <p>
 * <pre>
 *   0     1     2     3     4        5     6     7     8         9          10      11     12  13  14   15 16
 *   +-----+-----+-----+-----+--------+----+----+----+------+-----------+-------+----- --+-----+-----+-------+
 *   |   magic   code        |version | full length         | messageType| codec|compress|    RequestId       |
 *   +-----------------------+--------+---------------------+-----------+-----------+-----------+------------+
 *   |                                                                                                       |
 *   |                                         body                                                          |
 *   |                                                                                                       |
 *   |                                        ... ...                                                        |
 *   +-------------------------------------------------------------------------------------------------------+
 * 4B  magic code（魔法数）   1B version（版本）   4B full length（消息长度）    1B messageType（消息类型）
 * 1B compress（压缩类型） 1B codec（序列化类型）    4B  requestId（请求的Id）
 * body（object类型数据）
 * </pre>
 *
 * @author WangTao
 * @createTime on 2020/10/2
 * @see <a href="https://zhuanlan.zhihu.com/p/95621344">LengthFieldBasedFrameDecoder解码器</a>
 */

@Slf4j  // 使用@Slf4j注解自动生成日志记录器
public class RpcMessageEncoder extends MessageToByteEncoder<RpcMessage> {
    // 创建一个原子整数，记录每次编码时的序列号
    private static final AtomicInteger ATOMIC_INTEGER = new AtomicInteger(0);

    @Override
    protected void encode(ChannelHandlerContext ctx, RpcMessage rpcMessage, ByteBuf out) {
        try {
            // 写入魔数（Magic Number）标识协议开始，通常用来验证协议的一致性
            out.writeBytes(RpcConstants.MAGIC_NUMBER);

            // 写入版本号，标识协议版本
            out.writeByte(RpcConstants.VERSION);

            // 留出位置写入总长度（full length），初始为4个字节，稍后会更新该字段
            out.writerIndex(out.writerIndex() + 4);

            // 写入消息类型（如请求或响应）
            byte messageType = rpcMessage.getMessageType();
            out.writeByte(messageType);

            // 写入编码方式（例如，是否使用JSON、Protobuf等进行序列化）
            out.writeByte(rpcMessage.getCodec());

            // 写入压缩方式（例如，是否使用GZIP等进行压缩）
            out.writeByte(CompressTypeEnum.GZIP.getCode());

            // 写入一个唯一的序列号，保证每个请求都有不同的ID
            out.writeInt(ATOMIC_INTEGER.getAndIncrement());

            // 初始化bodyBytes和总长度（full length）
            byte[] bodyBytes = null;
            int fullLength = RpcConstants.HEAD_LENGTH;  // 头部长度是固定的，通常是协议头的长度

            // 如果消息类型不是心跳请求或心跳响应，则需要进行数据序列化和压缩
            if (messageType != RpcConstants.HEARTBEAT_REQUEST_TYPE
                    && messageType != RpcConstants.HEARTBEAT_RESPONSE_TYPE) {

                // 获取编码器类型的名称（例如JSON或Protobuf）
                String codecName = SerializationTypeEnum.getName(rpcMessage.getCodec());
                log.info("codec name: [{}] ", codecName);

                // 通过扩展加载器获取对应的序列化器
                Serializer serializer = ExtensionLoader.getExtensionLoader(Serializer.class)
                        .getExtension(codecName);

                // 序列化消息体数据
                bodyBytes = serializer.serialize(rpcMessage.getData());

                // 获取压缩类型名称（例如GZIP）
                String compressName = CompressTypeEnum.getName(rpcMessage.getCompress());

                // 通过扩展加载器获取对应的压缩器
                Compress compress = ExtensionLoader.getExtensionLoader(Compress.class)
                        .getExtension(compressName);

                // 压缩序列化后的字节数据
                bodyBytes = compress.compress(bodyBytes);

                // 更新完整消息长度
                fullLength += bodyBytes.length;
            }

            // 如果有bodyBytes，写入消息体
            if (bodyBytes != null) {
                out.writeBytes(bodyBytes);
            }

            // 获取当前的写指针位置
            int writeIndex = out.writerIndex();

            // 将之前留出的4个字节位置更新为完整长度
            // 这里通过调整写指针，重新写入消息的总长度字段
            out.writerIndex(writeIndex - fullLength + RpcConstants.MAGIC_NUMBER.length + 1);
            out.writeInt(fullLength);

            // 恢复写指针到原来的位置
            out.writerIndex(writeIndex);

        } catch (Exception e) {
            // 异常处理：编码过程中发生错误时，记录日志并输出错误信息
            log.error("Encode request error!", e);
        }
    }
}


