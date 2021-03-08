package marcoalunno.com.arias_and_inteludes;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Build;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import android.util.AttributeSet;
import android.view.View;

public class Graph extends View {
    public int width = 0;
    public int height = 0;
    private Paint paint = new Paint();
    private Path path = new Path();
    public int startY = 0;
    public int endY = 0;
    public float rateX = 0;
    public float rateY = 0;

    public Graph(Context context) {
        super(context);
        init(null);
    }

    public Graph(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public Graph(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public Graph(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }

    private void init(@Nullable AttributeSet set){
    }

    @Override
    protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        width = canvas.getWidth();
        height = canvas.getHeight();
        canvas.drawColor(Color.GRAY);

        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(10);

        canvas.drawLine(0,0,0, height, paint); //vertical
        canvas.drawLine(0, height, width, height, paint); //horizontal

        paint.setStrokeWidth(1);
        for (int i = height; i > 0; i-=height/5){
            canvas.drawLine(0, i, width, i, paint);
        }
        for (int i = 0; i < width; i+=width/5){
            canvas.drawLine(i,0, i, height, paint);
        }

        paint.setStrokeWidth(3);
        paint.setColor(Color.BLUE);
        path.moveTo(0, startY);
        path.quadTo(rateX, rateY, width, endY);
        canvas.drawPath(path, paint);

        //paint.setStyle(Paint.Style.FILL);
        //paint.setColor(Color.RED);
        //canvas.drawCircle(rateX, rateY, 7, paint);
    }

    public void reDraw() {
        path.reset();
        invalidate();
    }
}