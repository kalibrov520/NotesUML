package com.group.notes;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import static com.group.notes.DataUtils.NEW_NOTE_REQUEST;
import static com.group.notes.DataUtils.NOTE_BODY;
import static com.group.notes.DataUtils.NOTE_COLOUR;
import static com.group.notes.DataUtils.NOTE_FONT_SIZE;
import static com.group.notes.DataUtils.NOTE_HIDE_BODY;
import static com.group.notes.DataUtils.NOTE_REQUEST_CODE;
import static com.group.notes.DataUtils.NOTE_TITLE;


public class EditActivity extends AppCompatActivity  {

    // компоненты макета activity_edit
    private EditText titleEdit, bodyEdit;
    private RelativeLayout relativeLayoutEdit;
    private Toolbar toolbar;


    private InputMethodManager imm;
    private Bundle bundle;

    private String[] fontSizeNameArr; // Font size names string array

    // дефолтные значения для всех заметок
    private String colour = "#FFFFFF";
    private int fontSize = 18;
    private Boolean hideBody = true;

    private AlertDialog saveChangesDialog;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        if (Build.VERSION.SDK_INT >= 18)
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_USER);


        else
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);

        fontSizeNameArr = getResources().getStringArray(R.array.fontSizeNames);

        setContentView(R.layout.activity_edit);

        // Init layout components
        toolbar = (Toolbar)findViewById(R.id.toolbarEdit);
        titleEdit = (EditText)findViewById(R.id.titleEdit);
        bodyEdit = (EditText)findViewById(R.id.bodyEdit);
        relativeLayoutEdit = (RelativeLayout)findViewById(R.id.relativeLayoutEdit);

        imm = (InputMethodManager) this.getSystemService(INPUT_METHOD_SERVICE);

        if (toolbar != null)
            initToolbar();


        // получаем инфу от MainActivity
        bundle = getIntent().getExtras();

        if (bundle != null) {
            // собственно если кликнули по уже созданной заметке, то извлекаем данные
            if (bundle.getInt(NOTE_REQUEST_CODE) != NEW_NOTE_REQUEST) {
                colour = bundle.getString(NOTE_COLOUR);
                fontSize = bundle.getInt(NOTE_FONT_SIZE);
                hideBody = bundle.getBoolean(NOTE_HIDE_BODY);

                titleEdit.setText(bundle.getString(NOTE_TITLE));
                bodyEdit.setText(bundle.getString(NOTE_BODY));
                bodyEdit.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);

            }

            // если заметка новая, то делаем тагерт на Title и показываем клавиатуру
            else if (bundle.getInt(NOTE_REQUEST_CODE) == NEW_NOTE_REQUEST) {
                titleEdit.requestFocus();
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
            }

            // цвет задника в заметке всегда дефолтный
            relativeLayoutEdit.setBackgroundColor(Color.parseColor(colour));
        }

        initDialogs(this);
    }


    protected void initToolbar() {
        toolbar.setTitle("");

        // помещаем на тулбар Стрелочку-возврата и обрабатываем нажатие на нее
        toolbar.setNavigationIcon(R.drawable.abc_ic_ab_back_material);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

    protected void initDialogs(Context context) {
        saveChangesDialog = new AlertDialog.Builder(context)
                .setMessage(R.string.dialog_save_changes)
                .setPositiveButton(R.string.yes_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //наличие Заголовка делаем обязательным
                        if (!isEmpty(titleEdit) || !isEmpty(bodyEdit))
                            saveChanges();
                        else
                            toastEditTextCannotBeEmpty();
                    }
                })
                .setNegativeButton(R.string.no_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        finish();
                        overridePendingTransition(0, 0);
                    }
                })
                .create();
    }


    protected void saveChanges() {
        Intent intent = new Intent();

        // пакуем все данные и возвращаемся к Мэйну
        intent.putExtra(NOTE_TITLE, titleEdit.getText().toString());
        intent.putExtra(NOTE_BODY, bodyEdit.getText().toString());
        intent.putExtra(NOTE_COLOUR, colour);
        intent.putExtra(NOTE_FONT_SIZE, fontSize);
        intent.putExtra(NOTE_HIDE_BODY, hideBody);

        setResult(RESULT_OK, intent);

        imm.hideSoftInputFromWindow(titleEdit.getWindowToken(), 0);

        finish();
        overridePendingTransition(0, 0);
    }


    @Override
    public void onBackPressed() {
        //для новых заметок
        if (bundle.getInt(NOTE_REQUEST_CODE) == NEW_NOTE_REQUEST)
            saveChangesDialog.show();


        else {
            if (!isEmpty(titleEdit) || !isEmpty(bodyEdit)) {
                if (!(titleEdit.getText().toString().equals(bundle.getString(NOTE_TITLE))) ||
                    !(bodyEdit.getText().toString().equals(bundle.getString(NOTE_BODY))) ||
                    !(colour.equals(bundle.getString(NOTE_COLOUR))) ||
                    fontSize != bundle.getInt(NOTE_FONT_SIZE) ||
                    hideBody != bundle.getBoolean(NOTE_HIDE_BODY)) {

                    saveChanges();
                }

                else {
                    imm.hideSoftInputFromWindow(titleEdit.getWindowToken(), 0);

                    finish();
                    overridePendingTransition(0, 0);
                }
            }

            else
                toastEditTextCannotBeEmpty();
        }
    }


    protected boolean isEmpty(EditText editText) {
        return editText.getText().toString().trim().length() == 0;
    }

    protected void toastEditTextCannotBeEmpty() {
        Toast toast = Toast.makeText(getApplicationContext(),
                getResources().getString(R.string.toast_edittext_cannot_be_empty),
                Toast.LENGTH_LONG);
        toast.show();
    }



}
