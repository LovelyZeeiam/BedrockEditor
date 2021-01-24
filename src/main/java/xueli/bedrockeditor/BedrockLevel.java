package xueli.bedrockeditor;

import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.stream.NBTInputStream;
import com.flowpowered.nbt.stream.NBTOutputStream;

import java.io.Closeable;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * �����ǻ��Ұ�浵�����Ķ��󣬿��ִ����￪ʼ
 * @author Xueli
 */
public class BedrockLevel implements Closeable {

    // �浵���ڵ�ַ
    private String path;
    // �浵�е�db�ļ��ж�Ӧ�Ķ��� (ʵ���Ͼ���һ��levelDB���ݿ�)
    private LevelDatabase db;
    // level.dat
    private CompoundTag levelDat;
    // level.dat��ǰ8���ֽ� �����˰汾��֮�����Ϣ
    private byte[] levelDatVersion = new byte[8];

    /**
     * @param path �浵��·��
     * @throws IOException ���浵�Ķ�ȡ���ִ���ʱ���׳�����쳣
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
     * ���沢�ر�����浵
     * @throws IOException �����ֶ�д����ʱ�����׳�����쳣
     */
    @Override
    public void close() throws IOException {
        // ���沢�ر�db�ļ���
        db.close();

        // �洢level.dat�ļ�
        FileOutputStream levelDatOut = new FileOutputStream(this.path + "/level.dat");
        // ��ǰ8���ֽ�ԭ�ⲻ��д��ȥ
        levelDatOut.write(levelDatVersion);
        // д����NBT
        NBTOutputStream levelDatNBTOut = new NBTOutputStream(levelDatOut);
        levelDatNBTOut.writeTag(levelDat);
        // �ر��ļ�
        levelDatNBTOut.close();

    }

}
