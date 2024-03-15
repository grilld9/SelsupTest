import com.google.gson.Gson;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CrptApi {

    private final int requestLimit;
    private final OkHttpClient client;
    private final Gson jsonConverter;
    private final Timer timer;
    private final TimerTask timerTask;
    private final long periodMills;
    private final Semaphore semaphore;
    private static final String BASE_URL = "https://ismp.crpt.ru/api/v3/lk/documents/create";
    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.semaphore = new Semaphore(requestLimit, true);
        this.periodMills = CrptApi.toMills(timeUnit);
        this.requestLimit = requestLimit;
        this.client = new OkHttpClient();
        this.jsonConverter = new Gson();
        this.timer = new Timer();
        this.timerTask = new TimerTask() {
            @Override
            public void run() {
                semaphore.release(requestLimit);
            }
        };
    }


    public Response postDocument(Object document, String signature)
        throws IOException, InterruptedException {
        synchronized (this) {
            if (semaphore.availablePermits() == requestLimit) {
                timer.schedule(timerTask, periodMills);
            }
        }
        semaphore.acquire();
        String documentInJson = jsonConverter.toJson(document);
        RequestBody requestBody = RequestBody.create(documentInJson, MediaType.get("application/json"));
        Request request = new Request.Builder()
            .url(BASE_URL)
            .post(requestBody)
            .addHeader("Signature", signature)
            .build();
        return client.newCall(request).execute();
    }

    private static long toMills(TimeUnit timeUnit) {
        if (timeUnit == TimeUnit.DAYS) {
            return 24 * 60 * 60 * 1000L;
        } else if (timeUnit == TimeUnit.HOURS) {
            return 60 * 60 * 1000L;
        } else if (timeUnit == TimeUnit.MINUTES) {
            return 60 * 1000L;
        } else if (timeUnit == TimeUnit.SECONDS) {
            return 1000L;
        } else {
            return 1L;
        }
    }
}
