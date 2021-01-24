package xueli.bedrockeditor;

import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.stream.NBTInputStream;
import com.flowpowered.nbt.stream.NBTOutputStream;

import java.io.Closeable;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 这里是基岩版存档整个的对象，开局从这里开始
 * @author Xueli
 */
public class BedrockLevel implements Closeable {

    // 存档所在地址
    private String path;
    // 存档中的db文件夹对应的对象 (实际上就是一个levelDB数据库)
    private LevelDatabase db;
    // level.dat
    private CompoundTag levelDat;
    // level.dat的前8个字节 代表了版本号之类的信息
    private byte[] levelDatVersion = new byte[8];

    /**
     * @param path 存档的路径
     * @throws IOException 当存档的读取出现错误时，抛出这个异常
     */
    public BedrockLevel(String path) throws IOException {
        this.path = path;
        this.db = new LevelDatabase(path + "/db/");

        FileInputStream leveldatIn = new FileInputStream(this.path + "/level.dat");
        leveldatIn.read(levelDatVersion, 0,8);
        NBTInputStream leveldatInNBT = new NBTInputStream(leveldatIn);
        levelDat = (CompoundTag) leveldatInNBT.readTag();
        leveldatInNBT.close();

    }

    public LevelDatabase getDb() {
        return db;
    }

    public CompoundTag getLevelDat() {
        return levelDat;
    }

    public String getPath() {
        return path;
    }

    /**
     * 保存并关闭这个存档
     * @throws IOException 当出现读写错误时，会抛出这个异常
     */
    @Override
    public void close() throws IOException {
        // 保存并关闭db文件夹
        db.close();

        // 存储level.dat文件
        FileOutputStream levelDatOut = new FileOutputStream(this.path + "/level.dat");
        // 将前8个字节原封不动写回去
        levelDatOut.write(levelDatVersion);
        // 写正文NBT
        NBTOutputStream levelDatNBTOut = new NBTOutputStream(levelDatOut);
        levelDatNBTOut.writeTag(levelDat);
        // 关闭文件
        levelDatNBTOut.close();

    }

}
