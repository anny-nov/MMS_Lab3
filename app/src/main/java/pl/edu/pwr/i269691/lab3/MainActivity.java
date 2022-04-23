package pl.edu.pwr.i269691.lab3;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private Button button;
    private Button rec;
    private ImageView imageView;
    private TextView textView;
    private RadioButton btn1;
    private RadioButton btn2;
    TextRecognizer recognizer;
    ObjectDetector objectDetector;
    InputImage image;
    Uri uri;
    Bitmap imageBitmap;
    boolean check = true;
    int SELECT_IMAGE_CODE = 101;
    int GIVE_PERMISSIONS = 102;
    int MAKE_PHOTO_CODE = 103;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        button=(Button) findViewById(R.id.btn);
        rec=(Button) findViewById(R.id.button);
        imageView=(ImageView) findViewById(R.id.imageView);
        textView=(TextView) findViewById(R.id.textView);
        btn1 = (RadioButton) findViewById(R.id.radioButton3);
        btn2 = (RadioButton) findViewById(R.id.radioButton4);
        recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        ObjectDetectorOptions options =
                new ObjectDetectorOptions.Builder().setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
                        .enableMultipleObjects()
                        .enableClassification()
                        .build();
        objectDetector = ObjectDetection.getClient(options);

    }

    @Override
    protected void onStart() {
        super.onStart();
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(check) {
                    if(ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.WRITE_EXTERNAL_STORAGE)==PackageManager.PERMISSION_DENIED)
                        ActivityCompat.requestPermissions(MainActivity.this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},GIVE_PERMISSIONS);
                    else {
                        PickImage();
                    }
                } else {
                    if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, GIVE_PERMISSIONS);
                    }
                    else {
                        TakePicture();
                }
                }
            }
        });
        rec.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(MainActivity.this, "It is clickable!", Toast.LENGTH_LONG).show();
                //Bitmap bitm = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
                //InputImage image = InputImage.fromBitmap(bitm,0);
                ConvertBitmap();
                Task<Text> result =
                        recognizer.process(image)
                                .addOnSuccessListener(new OnSuccessListener<Text>() {
                                    @Override
                                    public void onSuccess(Text visionText) {
                                        String text = visionText.getText();
                                        textView.setText(text);
                                    }
                                })
                                .addOnFailureListener(
                                        new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Toast.makeText(MainActivity.this, "Something goes wrong!", Toast.LENGTH_LONG).show();
                                            }
                                        });
            }
        });
    }

    private void ObjectDetection(){
        ConvertBitmap();
        objectDetector.process(image)
                .addOnSuccessListener(
                        new OnSuccessListener<List<DetectedObject>>() {
                            @Override
                            public void onSuccess(List<DetectedObject> detectedObjects) {
                                for (DetectedObject detectedObject : detectedObjects) {
                                    Rect boundingBox = detectedObject.getBoundingBox();
                                    Bitmap bitm = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
                                    bitm = drawDetectionResult(bitm, detectedObjects);
                                    imageView.setImageBitmap(bitm);
                                }
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // Task failed with an exception
                                // ...
                            }
                        });
    }

    private Bitmap drawDetectionResult(Bitmap bitm, List<DetectedObject> detectedObjects) {
        Bitmap final_bitmap = bitm.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(final_bitmap);
        Paint pen = new Paint();
        pen.setTextAlign(Paint.Align.LEFT);
        for (DetectedObject detectedObject : detectedObjects) {
            pen.setColor(Color.RED);
            pen.setStrokeWidth(2F);
            pen.setStyle(Paint.Style.STROKE);
            canvas.drawRect(detectedObject.getBoundingBox(),pen);
        }
        return final_bitmap;
    }

    private void ConvertBitmap() {
        Bitmap bitm = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
        image = InputImage.fromBitmap(bitm,0);
    }


    private void TakePicture() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent,MAKE_PHOTO_CODE);
    }

    private void PickImage() {
        Intent intent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        startActivityForResult(intent,SELECT_IMAGE_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == SELECT_IMAGE_CODE && resultCode == RESULT_OK && data != null) {
            uri = data.getData();
            imageView.setImageURI(uri);

        }
        if (requestCode == MAKE_PHOTO_CODE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            imageBitmap = (Bitmap) extras.get("data");
            imageView.setImageBitmap(imageBitmap);
        }
        ObjectDetection();
        super.onActivityResult(requestCode, resultCode, data);
    }


    public void onRadioButtonClicked(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();

        // Check which radio button was clicked
        switch(view.getId()) {
            case R.id.radioButton3:
                if (checked)
                    check = true;
                    break;
            case R.id.radioButton4:
                if (checked)
                    check = false;
                    break;
        }
    }
}