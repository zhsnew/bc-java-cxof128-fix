package org.bouncycastle.mls.TreeKEM;

import org.bouncycastle.mls.codec.Capabilities;
import org.bouncycastle.mls.codec.Credential;
import org.bouncycastle.mls.codec.Extension;
import org.bouncycastle.mls.codec.MLSInputStream;
import org.bouncycastle.mls.codec.MLSOutputStream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LeafNode
        implements MLSInputStream.Readable, MLSOutputStream.Writable
{
    byte[] encryption_key;
    byte[] signature_key;
    Credential credential;
    Capabilities capabilities;
    LeafNodeSource leaf_node_source;

    //in switch
    LifeTime lifeTime;
    byte[] parent_hash;

    List<Extension> extensions;
    /* SignWithLabel(., "LeafNodeTBS", LeafNodeTBS) */
    byte[] signature; // not in TBS

    public LeafNode(MLSInputStream stream) throws IOException
    {
        encryption_key = stream.readOpaque();
        signature_key = stream.readOpaque();
        credential = (Credential) stream.read(Credential.class);
        capabilities = (Capabilities) stream.read(Capabilities.class);
        leaf_node_source = LeafNodeSource.values()[(byte) stream.read(byte.class)];
        switch (leaf_node_source)
        {
            case KEY_PACKAGE:
                lifeTime = (LifeTime) stream.read(LifeTime.class);
                break;
            case UPDATE:
                break;
            case COMMIT:
                parent_hash = stream.readOpaque();
                break;
        }
        extensions = new ArrayList<>();
        stream.readList(extensions, Extension.class);
        signature = stream.readOpaque();


    }

    @Override
    public void writeTo(MLSOutputStream stream) throws IOException
    {
        stream.writeOpaque(encryption_key);
        stream.writeOpaque(signature_key);
        stream.write(credential);
        stream.write(capabilities);
        stream.write(leaf_node_source);
        switch (leaf_node_source)
        {
            case KEY_PACKAGE:
                stream.write(lifeTime);
                break;
            case UPDATE:
                break;
            case COMMIT:
                stream.writeOpaque(parent_hash);
                break;
        }
        stream.writeList(extensions);
        stream.writeOpaque(signature);
    }
}

enum LeafNodeSource
        implements MLSInputStream.Readable, MLSOutputStream.Writable
{
    RESERVED((byte) 0),
    KEY_PACKAGE((byte) 1),
    UPDATE((byte) 2),
    COMMIT((byte) 3);

    final byte value;

    LeafNodeSource(byte value)
    {
        this.value = value;
    }

    @Override
    public void writeTo(MLSOutputStream stream) throws IOException
    {
        stream.write(value);
    }
}

