package io.lingpai.tutor.p2p;

import io.lingpai.tutor.Block;
import io.lingpai.tutor.Transaction;

import java.util.List;

public interface MessageListener {
    void getBlocks(String startHash) throws InterruptedException;
    void newBlock(Block block);
    void newTransaction(Transaction transaction);
    void syncBlocks(List<Block> blockList);
}
