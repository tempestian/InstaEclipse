package ps.reso.instaeclipse.mods.media;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.ArrayList;

public class PostDownloadContextMenuHookTest {

    private enum FakeOptionEnum { DOWNLOAD, PROMOTE_DEBUG }
    private enum FakeButtonStyleEnum { NORMAL, DESTRUCTIVE }
    private static final class FakeMenuCreator {}

    @Test
    public void resolvesStandardAndSwappedEnumOrderingsForIg449() {
        Class<?>[] standard = new Class<?>[]{
                FakeButtonStyleEnum.class,
                FakeOptionEnum.class,
                FakeMenuCreator.class,
                CharSequence.class,
                ArrayList.class
        };
        PostDownloadContextMenuHook.AddButtonMatch m1 = PostDownloadContextMenuHook.resolveAddButtonParams(
                standard, FakeOptionEnum.class, FakeMenuCreator.class);
        assertNotNull(m1);
        assertEquals(0, m1.idxEnum);
        assertEquals(1, m1.idxOption);
        assertEquals(2, m1.idxSelf);
        assertEquals(3, m1.idxText);
        assertEquals(4, m1.idxList);
        assertEquals(4, m1.score);

        // IG 449 regression order: MediaOption$Option precedes button-type enum
        Class<?>[] swapped = new Class<?>[]{
                FakeOptionEnum.class,
                FakeButtonStyleEnum.class,
                FakeMenuCreator.class,
                String.class,
                ArrayList.class,
                int.class
        };
        PostDownloadContextMenuHook.AddButtonMatch m2 = PostDownloadContextMenuHook.resolveAddButtonParams(
                swapped, FakeOptionEnum.class, FakeMenuCreator.class);
        assertNotNull(m2);
        assertEquals(1, m2.idxEnum);
        assertEquals(0, m2.idxOption);
        assertEquals(2, m2.idxSelf);
        assertEquals(3, m2.idxText);
        assertEquals(4, m2.idxList);
        assertEquals(4, m2.score);
    }

    @Test
    public void rejectsOverloadWithoutCharSequenceLabelParam() {
        Class<?>[] withoutLabel = new Class<?>[]{
                FakeOptionEnum.class,
                FakeButtonStyleEnum.class,
                FakeMenuCreator.class,
                int.class,
                ArrayList.class
        };
        assertNull(PostDownloadContextMenuHook.resolveAddButtonParams(
                withoutLabel, FakeOptionEnum.class, FakeMenuCreator.class));
    }
}
