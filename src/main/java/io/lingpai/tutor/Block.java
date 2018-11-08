package io.lingpai.tutor;


import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;


//单个区块
@Data
@Accessors(chain = true)
@Slf4j
@NoArgsConstructor
public class Block {
    private long height; //区块的高度，创世块是0
    private String hash;
    private String previousHash; //上一个区块的哈希
    public String merkleRoot; //交易数组对应的默克尔树， 比整个数组取哈希要快
    public ArrayList<Transaction> transactions = new ArrayList<>();  //区块数据就是交易的列表
    private long timeStamp; //unix时间戳，单位秒
    private int difficulty; //该区块打包时的难度, 就是hash值的前多少位是0, 比特币创世区块的难度是10个0， 现在已经是18个0

    //在挖矿时根据nonce来取的
    private int nonce = 0;

    public Block(long index, String previousHash, int difficulty) {
        this.height = index;
        this.previousHash = previousHash;

        this.timeStamp = System.currentTimeMillis() / 1000l;
        this.difficulty = difficulty;
    }

    @JsonIgnore
    public boolean isValid() {
        return hash.equals(calHash());
    }

    private String calHash() {

        return Utils.applySha256(Long.toString(height) + previousHash +
                Long.toString(timeStamp) + Integer.toString(nonce) + Integer.toString(difficulty) +
                merkleRoot);
    }

    //当难度是5时，在2015年的13寸MBP上，几秒钟就能计算出来
    //下面的算法和比特币的挖矿流程是一样的
    public void mine(int difficulty) {

        merkleRoot = Utils.getMerkleRoot(transactions);
        hash = calHash();
        String target = new String(new char[difficulty]).replace('\0', '0');
        while (!hash.startsWith(target)) {
            //计算hash的这几个值里，只有nonce是变化的，其它的值在区块构建时已经固定下来了, 所以递增nonce就能让每次的hash不一样
            nonce++;
            hash = calHash();
        }
    }


    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Block)) {
            return false;
        }

        Block b = (Block) o;
        return b.getHash().equals(hash) && b.calHash().equals(b.calHash());
    }


    //把交易添加到区块中，添加前会检查交易的有效性
    public boolean addTransaction(Transaction transaction) {
        //process transaction and check if valid, unless block is genesis block then ignore.
        if (transaction == null) return false;

        //不是通过挖矿奖励的交易，就需要验证合法性
        if ((transaction.processTransaction() != true)) {
            log.error("failed to process Transaction: {}. Discarded.", transaction.getTransactionId());
            return false;
        }

        transactions.add(transaction);
        return true;
    }





}
