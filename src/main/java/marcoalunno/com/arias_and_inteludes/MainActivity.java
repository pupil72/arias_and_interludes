package marcoalunno.com.arias_and_inteludes;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.view.View;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private TextView intrada, aria1, percussionInterlude, aria2, thereminInterlude, aria3, salida;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        intrada = findViewById(R.id.prelude1);
        aria1 = findViewById(R.id.prelude2);
        percussionInterlude = findViewById(R.id.prelude3);
        aria2 = findViewById(R.id.prelude4);
        thereminInterlude = findViewById(R.id.prelude5);
        aria3 = findViewById(R.id.prelude6);
        salida = findViewById(R.id.prelude7);

        intrada.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), Intrada.class);
                startActivity(intent);
            }
        });

        aria1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), Aria1.class);
                startActivity(intent);
            }
        });

        percussionInterlude.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), PercussionDetectorRateController.class);
                startActivity(intent);
            }
        });

        aria2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), Aria2.class);
                startActivity(intent);

            }
        });

        thereminInterlude.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), Theremin.class);
                startActivity(intent);
            }
        });

        aria3.setOnClickListener((new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), Aria3.class);
                startActivity(intent);
            }
        }));

        salida.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), Salida.class);
                startActivity(intent);
            }
        });
    }
}