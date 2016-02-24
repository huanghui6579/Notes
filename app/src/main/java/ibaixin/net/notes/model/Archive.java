package ibaixin.net.notes.model;

/**
 * 笔记的分类文件夹
 * @author huanghui1
 * @update 2016/2/24 18:23
 * @version: 0.0.1
 */
public class Archive {
    private int id;
    
    private String name;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
