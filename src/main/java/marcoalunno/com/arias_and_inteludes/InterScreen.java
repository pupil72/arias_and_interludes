package marcoalunno.com.arias_and_inteludes;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.view.View;

import org.opencv.core.Point;

import java.util.ArrayList;

public class InterScreen extends View {
    private Paint paint = new Paint();
    public int colorCircle = Color.argb(150, 0,0,0) ;
    private int colorHead = Color.RED;
    public int colorLines = Color.TRANSPARENT;
    private ArrayList<Point> points = new ArrayList<>();
    public boolean isStopped;

    public InterScreen(Context context) {
        super(context);
    }

    public InterScreen(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onDraw(Canvas canvas){
        super.onDraw(canvas);

        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.OVERLAY);

        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(10);
        canvas.drawRect(0,0, canvas.getWidth(), canvas.getHeight(), paint);

        paint.setColor(colorLines);
        paint.setStrokeWidth(2);
        for (int i = 0; i < canvas.getWidth(); i = i+(canvas.getWidth()/11)) {
            canvas.drawLine(i, 0, i, canvas.getHeight(), paint);
        }

        paint.setStyle(Paint.Style.FILL);
        if (!isStopped) {
            for (int i = 0; i < points.size(); i++) {
                if (i == points.size() - 1) {
                    paint.setColor(colorHead);
                } else {
                    paint.setColor(colorCircle);
                }
                canvas.drawCircle((int) points.get(i).x, (int) points.get(i).y, 15, paint);
            }
        }
    }

    public void reDraw(int x, int y){
        if (points.size() < 10) {
            points.add(new Point(x, y));
        } else {
            points.add(new Point(x, y));
            points.remove(0);
        }
        invalidate();
    }

    public void clearCanvas(){
        points.clear();
        colorLines = Color.TRANSPARENT;
        invalidate();
        isStopped = true;
    }
}