import org.teavm.jso.JSBody;

public class JsInterop {
    @JSBody(params = {"message"}, script = "console.log(message)")
    public static native void log(String message);

    @JSBody(params = { }, script = "return navigator.userAgent;" )
    public static native String getUserAgent();

    @JSBody(params = { }, script = """
        let canvas = document.getElementById('canvas');
        let width = canvas.width;
        let height = canvas.height;
        return "" + width + ";" + height;
        """ )
    public static native String getCanvasSize();
}
