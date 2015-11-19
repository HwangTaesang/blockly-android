package com.google.blockly.model;

import android.test.AndroidTestCase;

import com.google.blockly.R;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * Tests for {@link Workspace}.
 */
public class WorkspaceTest extends AndroidTestCase {

    public static final String WORKSPACE_XML_START =
            "<xml xmlns=\"http://www.w3.org/1999/xhtml\">";

    public static final String WORKSPACE_XML_END = "</xml>";

    public static final String BAD_XML = "<type=\"xml_no_name\">";

    public static final String EMPTY_WORKSPACE =
            "<xml xmlns=\"http://www.w3.org/1999/xhtml\" />";

    private static ByteArrayInputStream assembleWorkspace(String interior) {
        return new ByteArrayInputStream(
                (WORKSPACE_XML_START + interior + WORKSPACE_XML_END).getBytes());
    }

    public void testXmlParsing() {
        // TODO: Move test_blocks.json to the testapp's resources once
        // https://code.google.com/p/android/issues/detail?id=64887 is fixed.
        Workspace.Builder bob = new Workspace.Builder(getContext());
        bob.addBlockDefinitions(R.raw.test_blocks);
        Workspace workspace = bob.build();
        workspace.loadFromXml(assembleWorkspace(""));
        assertEquals("Workspace should be empty", 0, workspace.getRootBlocks().size());
        workspace.loadFromXml(assembleWorkspace(BlockTestStrings.SIMPLE_BLOCK));
        assertEquals("Workspace should have one block", 1, workspace.getRootBlocks().size());
        workspace.loadFromXml(new ByteArrayInputStream(EMPTY_WORKSPACE.getBytes()));
        assertEquals("Workspace should be empty", 0, workspace.getRootBlocks().size());

        try {
            workspace.loadFromXml(assembleWorkspace(BAD_XML));
            fail("Should have thrown a BlocklyParseException.");
        } catch (BlocklyParserException expected) {
            // expected
        }
    }

    public void testSerialization() throws BlocklySerializerException {
        Workspace.Builder bob = new Workspace.Builder(getContext());
        bob.addBlockDefinitions(R.raw.test_blocks);
        Workspace workspace = bob.build();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        workspace.serializeToXml(os);
        assertEquals(EMPTY_WORKSPACE, os.toString());
    }
}
