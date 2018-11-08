package io.lingpai.tutor.p2p;

import com.fasterxml.jackson.annotation.JsonIgnore;

//p2p网络消息接口，目前就4种消息
public interface IMessage {
    byte GET_BLOCKS = 1;
    byte RETURN_BLOCKS = 2;
    byte NEW_BLOCK = 3;
    byte NEW_TRANSACTION = 4;

    byte getType();
    @JsonIgnore
    byte[] getBody();
}
