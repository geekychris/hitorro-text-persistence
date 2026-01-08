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

import java.io.File;
import java.io.IOException;

/**
 */

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
    private String m_name;
    private String m_store;
    private String m_rootPath;
    //transient field
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
        os.writeString(m_name);
        os.writeString(m_store);
        os.writeString(m_rootPath);
    }

    public void deserialize(HTObjectInputStream os)
            throws IOException, ClassNotFoundException, StoreException {
        int version = os.readInt();
        super.deserialize(os);

        switch (version) {
            case 1:
                m_name = os.readString();
                m_store = os.readString();
                m_rootPath = os.readString();
        }
    }

    public void init() {
        if (m_rootDir == null) {
            String val = JVSProperties.getProperties().resolveJsonVariable(m_rootPath);
            m_rootDir = new File(val);
            if (!FileUtil.ensureParentDirectories(m_rootDir, true)) {
                com.hitorro.basedms.Log.basedms.error("Unable to create root path for file system %s, path %s",
                        m_name, val);
            }
        }
    }

    public String getName() {
        return m_name;
    }

    public void setName(String name) {
        m_name = name;
    }

    public File getRootDir() {
        init();
        return m_rootDir;
    }

    public String getStore() {
        return m_store;
    }

    public void setStore(String store) {
        m_store = store;
    }

    public String getRootPath() {
        return m_rootPath;
    }

    public void setRootPath(String root) {
        m_rootPath = root;
    }

    public int getSerializationVersion() {
        return SerializationVersion;
    }
}
