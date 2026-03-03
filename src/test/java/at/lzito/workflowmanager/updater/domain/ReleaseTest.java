package at.lzito.workflowmanager.updater.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReleaseTest {

    private static Release release(String version) {
        return new Release(version, "https://example.com/download");
    }

    @Test
    void isNewerThan_patchIncrement() {
        assertTrue(release("1.0.8").isNewerThan("1.0.7"));
    }

    @Test
    void isNewerThan_minorIncrement() {
        assertTrue(release("1.1.0").isNewerThan("1.0.9"));
    }

    @Test
    void isNewerThan_majorIncrement() {
        assertTrue(release("2.0.0").isNewerThan("1.9.9"));
    }

    @Test
    void isNewerThan_sameVersion() {
        assertFalse(release("1.0.7").isNewerThan("1.0.7"));
    }

    @Test
    void isNewerThan_olderRelease() {
        assertFalse(release("1.0.6").isNewerThan("1.0.7"));
    }

    @Test
    void isNewerThan_vPrefix_stripped() {
        assertTrue(release("v1.0.8").isNewerThan("v1.0.7"));
    }

    @Test
    void nullVersion_throws() {
        assertThrows(NullPointerException.class,
                () -> new Release(null, "https://example.com"));
    }

    @Test
    void nullDownloadUrl_throws() {
        assertThrows(NullPointerException.class,
                () -> new Release("1.0.0", null));
    }

    @Test
    void equalsByVersionOnly_differentUrlsAreEqual() {
        Release a = new Release("1.0.7", "https://a.com/file.jar");
        Release b = new Release("1.0.7", "https://b.com/other.jar");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void notEqual_differentVersions() {
        assertNotEquals(release("1.0.7"), release("1.0.8"));
    }
}
