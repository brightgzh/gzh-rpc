package com.gzh.remoting.transport.netty.server;

import com.gzh.enums.CompressTypeEnum;
import com.gzh.enums.RpcResponseCodeEnum;
import com.gzh.enums.SerializationTypeEnum;
import com.gzh.factory.SingletonFactory;
import com.gzh.remoting.constants.RpcConstants;
import com.gzh.remoting.dto.RpcMessage;
import com.gzh.remoting.dto.RpcRequest;
import com.gzh.remoting.dto.RpcResponse;
import com.gzh.remoting.handler.RpcRequestHandler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NettyRpcServerHandler extends ChannelInboundHandlerAdapter {
    private final RpcRequestHandler rpcRequestHandler;

    public NettyRpcServerHandler() {
        this.rpcRequestHandler = SingletonFactory.getInstance(RpcRequestHandler.class);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            if (msg instanceof RpcMessage) {
                log.info("server receive msg:{}",msg);
                byte messageType = ((RpcMessage) msg).getMessageType();
                RpcMessage rpcMessage = new RpcMessage();
                rpcMessage.setCodec(SerializationTypeEnum.HESSIAN.getCode());
                rpcMessage.setCompress(CompressTypeEnum.GZIP.getCode());

                if (messageType == RpcConstants.HEARTBEAT_REQUEST_TYPE) {//处理只是心跳链接的请求
                    rpcMessage.setMessageType(RpcConstants.HEARTBEAT_RESPONSE_TYPE);
                    rpcMessage.setData(RpcConstants.PONG);
                } else {
                    RpcRequest rpcRequest = (RpcRequest)((RpcMessage) msg).getData();
                    Object result = rpcRequestHandler.handle(rpcRequest);//不是心跳确认，交由具体server解决
                    rpcMessage.setMessageType(RpcConstants.RESPONSE_TYPE);
                    if (ctx.channel().isActive() && ctx.channel().isWritable()) {//判断通道状态是否正常
                        RpcResponse<Object> response = RpcResponse.success(result, rpcRequest.getRequestId());
                        rpcMessage.setData(response);
                    } else {
                        RpcResponse<Object> fail = RpcResponse.fail(RpcResponseCodeEnum.FAIL);
                        rpcMessage.setData(fail);
                    }
                }
                ctx.writeAndFlush(rpcMessage).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
            }
        } finally {
            if (msg instanceof ByteBuf && ((ByteBuf) msg).refCnt() > 0) {
                ReferenceCountUtil.release(msg);
            }

        }
    }

    //检测当通道处于空闲状态时关闭，节省资源
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleState state = ((IdleStateEvent) evt).state();
            if (state == IdleState.READER_IDLE) {
                log.info("idle check happen,so close the channel");
                ctx.close();
            }
        } else {
            super.userEventTriggered(ctx,evt);
        }
    }

    //异常捕获
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.info("server catch exception");
        cause.printStackTrace();
        ctx.close();
    }
}
