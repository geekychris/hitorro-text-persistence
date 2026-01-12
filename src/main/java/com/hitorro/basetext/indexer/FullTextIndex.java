/*
 * Copyright (c) 2006-2025 Chris Collins
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.hitorro.basetext.indexer;

import com.hitorro.base.typesystem.GuidBaseType;
import com.hitorro.base.typesystem.accessors.GuidAccessor;
import com.hitorro.basedms.BaseTypeOnTriggerGeneric;
import com.hitorro.jsontypesystem.propreaders.JVSProperties;
import com.hitorro.util.io.FileUtil;
import com.hitorro.util.io.StoreException;
import com.hitorro.util.typesystem.HTObjectInputStream;
import com.hitorro.util.typesystem.HTObjectOutputStream;
import com.hitorro.util.typesystem.OnTrigger;
import com.hitorro.util.typesystem.annotation.ImplClassMeta;
import com.hitorro.util.typesystem.annotation.TypeClassMetaInfo;

import jakarta.persistence.*;
import java.io.File;
import java.io.IOException;

/**
 */
@Entity
@Table(name = "FullTextIndex")
@TypeClassMetaInfo(shortTypeName = TypeClassMetaInfo.Index,
        isView = false,
        isPersisted = true,
        schemaVersion = FullTextIndex.SerializationVersion,
        softLinkField = "name",
        guidAccessor = GuidAccessor.class,
        onTriggers = {@ImplClassMeta(className = BaseTypeOnTriggerGeneric.class, trigger = OnTrigger.TriggerType.OnNew),
                @ImplClassMeta(className = BaseTypeOnTriggerGeneric.class, trigger = OnTrigger.TriggerType.BeforeSave),
                @ImplClassMeta(className = BaseTypeOnTriggerGeneric.class, trigger = OnTrigger.TriggerType.BeforeDelete),
                @ImplClassMeta(className = BaseTypeOnTriggerGeneric.class, trigger = OnTrigger.TriggerType.BeforePersist),
                @ImplClassMeta(className = BaseTypeOnTriggerGeneric.class, trigger = OnTrigger.TriggerType.OnLoad)})

public class FullTextIndex extends GuidBaseType {
    public static final int SerializationVersion = 1;
    
    @Column(name = "name")
    private String name;
    
    @Column(name = "store")
    private String store;
    
    @Column(name = "rootPath")
    private String rootPath;
    
    @Transient
    private File m_rootDir;

    public FullTextIndex() {

    }

    public FullTextIndex(String name, String storeName, String rootPath) {
        setName(name);
        setStore(storeName);
        setRootPath(rootPath);
    }

    public void serialize(HTObjectOutputStream os)
            throws IOException, StoreException {
        os.writeInt(getSerializationVersion());
        super.serialize(os);
        os.writeString(name);
        os.writeString(store);
        os.writeString(rootPath);
    }

    public void deserialize(HTObjectInputStream os)
            throws IOException, ClassNotFoundException, StoreException {
        int version = os.readInt();
        super.deserialize(os);

        switch (version) {
            case 1:
                name = os.readString();
                store = os.readString();
                rootPath = os.readString();
        }
    }

    public void init() {
        if (m_rootDir == null) {
            String val = JVSProperties.getProperties().resolveJsonVariable(rootPath);
            m_rootDir = new File(val);
            if (!FileUtil.ensureParentDirectories(m_rootDir, true)) {
                com.hitorro.basedms.Log.basedms.error("Unable to create root path for file system %s, path %s",
                        name, val);
            }
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public File getRootDir() {
        init();
        return m_rootDir;
    }

    public String getStore() {
        return store;
    }

    public void setStore(String store) {
        this.store = store;
    }

    public String getRootPath() {
        return rootPath;
    }

    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }

    public int getSerializationVersion() {
        return SerializationVersion;
    }
}
