/*
 * Copyright 2015 Google Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.blockly.model;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Helper class for building a set of master blocks and then obtaining copies of them for use in
 * a workspace or toolbar.
 */
public class BlockFactory {
    private static final String TAG = "BlockFactory";

    private Resources mResources;
    private final HashMap<String, Block> mBlockTemplates = new HashMap<>();
    private final HashMap<String, WeakReference<Block>> mBlockRefs = new HashMap<>();

    /**
     * Create a factory with an initial set of blocks from json resources.
     *
     * @param context The context for loading resources.
     * @param blockSourceIds A list of JSON resources containing blocks.
     */
    public BlockFactory(Context context, int[] blockSourceIds) {
        this(context);
        if (blockSourceIds != null) {
            for (int i = 0; i < blockSourceIds.length; i++) {
                addBlocks(blockSourceIds[i]);
            }
        }
    }

    /**
     * Create a factory.
     *
     * @param context The context for loading resources.
     */
    public BlockFactory(Context context) {
        mResources = context.getResources();
    }

    public BlockFactory(final InputStream source) {
        loadBlocks(source);
    }

    /**
     * Adds a block to the set of blocks that can be created.
     *
     * @param block The master block to add.
     */
    public void addBlockTemplate(Block block) {
        if (mBlockTemplates.containsKey(block.getName())) {
            Log.i(TAG, "Replacing block: " + block.getName());
        }
        mBlockTemplates.put(block.getName(), new Block.Builder(block).build());
    }

    /**
     * Removes a block type from the factory. If the Block is still in use by the workspace this
     * could cause a crash if the user tries to load a new block of this type.
     *
     * @param prototypeName The name of the block to remove.
     *
     * @return The master block that was removed or null if it wasn't found.
     */
    public Block removeBlockTemplate(String prototypeName) {
        return mBlockTemplates.remove(prototypeName);
    }

    /**
     * Creates a block of the specified type using one of the master blocks known to this factory.
     * If the prototypeName is not one of the known block types null will be returned instead.
     *
     * @param prototypeName The name of the block type to create.
     * @param uuid The id of the block if loaded from XML; null otherwise.
     *
     * @return A new block of that type or null.
     */
    public Block obtainBlock(String prototypeName, @Nullable String uuid) {
        // First search for any existing instance
        Block block;
        if (uuid != null) {
            WeakReference<Block> ref = mBlockRefs.get(uuid);
            if (ref != null) {
                block = ref.get();
                if (block != null) {
                    if (!block.getName().equals(prototypeName)) {
                        throw new IllegalArgumentException("Block with given UUID \"" + uuid
                                + "\" found. Prototype name \"" + prototypeName + "\" does not "
                                + "match existing block type \"" + block.getName() + "\".");
                    }
                    return block;
                }
            }
        }

        // Existing instance not found.  Constructing a new one.
        if (!mBlockTemplates.containsKey(prototypeName)) {
            Log.w(TAG, "Block " + prototypeName + " not found.");
            return null;
        }
        Block.Builder builder = new Block.Builder(mBlockTemplates.get(prototypeName));
        if (uuid != null) {
            builder.setUuid(uuid);
        }
        block = builder.build();
        mBlockRefs.put(block.getId(), new WeakReference<Block>(block));
        return block;
    }

    /**
     * @return The list of known blocks that can be created.
     */
    public List<Block> getAllBlocks() {
        return new ArrayList<>(mBlockTemplates.values());
    }

    /**
     * Loads and adds block templates from a resource.
     *
     * @param resId The id of the JSON resource to load blocks from.
     *
     * @return Number of blocks added to the factory.
     */
    public int addBlocks(int resId) {
        InputStream blockIs = mResources.openRawResource(resId);
        return loadBlocks(blockIs);
    }

    /**
     * Loads and adds block templates from a string.
     *
     * @param json_string The JSON string to load blocks from.
     *
     * @return Number of blocks added to the factory.
     */
    public int addBlocks(String json_string) {
        final InputStream blockIs = new ByteArrayInputStream(json_string.getBytes());
        return loadBlocks(blockIs);
    }


    /**
     * Loads and adds block templates from an input stream.
     *
     * @param is The json stream to read blocks from.
     *
     * @return Number of blocks added to the factory.
     */
    public int addBlocks(InputStream is) {
        return loadBlocks(is);
    }

    /**
     * Removes all blocks from the factory.
     */
    public void clear() {
        mBlockTemplates.clear();
        mBlockRefs.clear();
    }

    /**
     * Removes references to previous blocks, ensuring future blocks will be new instances.
     */
    public void clearPriorBlockReferences() {
        mBlockRefs.clear();
    }

    /** @return Number of blocks added to the factory. */
    private int loadBlocks(InputStream blockIs) {
        int blockAddedCount = 0;
        try {
            int size = blockIs.available();
            byte[] buffer = new byte[size];
            blockIs.read(buffer);

            String json = new String(buffer, "UTF-8");
            JSONArray blocks = new JSONArray(json);
            for (int i = 0; i < blocks.length(); i++) {
                JSONObject block = blocks.getJSONObject(i);
                String id = block.optString("id");
                if (!TextUtils.isEmpty(id)) {
                    mBlockTemplates.put(id, Block.fromJson(id, block));
                    ++blockAddedCount;
                } else {
                    throw new IllegalArgumentException("Block " + i
                            + " has no id and cannot be loaded.");
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error reading stream.", e);
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing JSON.", e);
        }

        return blockAddedCount;
    }
}
