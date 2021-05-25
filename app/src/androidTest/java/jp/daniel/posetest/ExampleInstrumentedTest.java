package jp.daniel.posetest;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    /*
    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("jp.daniel.posetest", appContext.getPackageName());
    }
    */


    @Test
    public void test_json() {
        String j =
                "{"
                        +"\n^id^: ^aa-bb-cc^,"
                        +"\n^urls^: {"
                        +"\n^tf^: null,"
                        +"\n^tfjs^: ^https://dummy.net/tfjs_model/model.json^,"
                        +"\n^tflite^: null,"
                        +"\n^website^: ^https://dummy.net/index.html^,"
                        +"\n^labels^: ^https://dummy.net/map.json^"
                        +"\n}"
                        +"\n}";
        j = j.replace('^', (char)34);

        ModelMetaData m = ModelMetaData.fromJasonString(j);
        assertEquals(m.id, "aa-bb-cc");
        assertEquals(m.urls.tfjs, "https://dummy.net/tfjs_model/model.json");
        assertEquals(m.urls.website, "https://dummy.net/index.html");
        assertEquals(m.urls.labels, "https://dummy.net/map.json");
    }
    @Test
    public void test_json_array() {
        String j =
                "[{"
                        + "\n^id^: ^aa-bb-cc^,"
                        + "\n^urls^: {"
                        + "\n^tf^: null,"
                        + "\n^tfjs^: ^https://dummy.net/tfjs_model/model.json^,"
                        + "\n^tflite^: null,"
                        + "\n^website^: ^https://dummy.net/index.html^,"
                        + "\n^labels^: ^https://dummy.net/map.json^"
                        + "\n}"
                        + "\n},"
                        + "{"
                        + "\n^id^: ^dd-ee-ff^,"
                        + "\n^urls^: {"
                        + "\n^tf^: null,"
                        + "\n^tfjs^: ^https2://dummy.net/tfjs_model/model.json^,"
                        + "\n^tflite^: null,"
                        + "\n^website^: ^https2://dummy.net/index.html^,"
                        + "\n^labels^: ^https2://dummy.net/map.json^"
                        + "\n}"
                        + "\n}]";

        j = j.replace('^', (char) 34);
        ModelMetaData[] m = ModelMetaData.arrayFromJasonString(j);
        assertEquals(m[0].id, "aa-bb-cc");
        assertEquals(m[0].urls.tfjs, "https://dummy.net/tfjs_model/model.json");
        assertEquals(m[0].urls.website, "https://dummy.net/index.html");
        assertEquals(m[0].urls.labels, "https://dummy.net/map.json");

        assertEquals(m[1].id, "dd-ee-ff");
        assertEquals(m[1].urls.tfjs, "https2://dummy.net/tfjs_model/model.json");
        assertEquals(m[1].urls.website, "https2://dummy.net/index.html");
        assertEquals(m[1].urls.labels, "https2://dummy.net/map.json");
    }
}