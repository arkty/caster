package ga.arkty.caster;

import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

/**
 * Author: Andrey Khitryy
 * Email: andrey.khitryy@gmail.com
 */

public class BaseActivity extends AppCompatActivity {

    protected void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
