package com.group.notes;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;

import static com.group.notes.DataUtils.BACKUP_FILE_NAME;
import static com.group.notes.DataUtils.BACKUP_FOLDER_PATH;
import static com.group.notes.DataUtils.NEW_NOTE_REQUEST;
import static com.group.notes.DataUtils.NOTES_FILE_NAME;
import static com.group.notes.DataUtils.NOTE_BODY;
import static com.group.notes.DataUtils.NOTE_COLOUR;
import static com.group.notes.DataUtils.NOTE_FAVOURED;
import static com.group.notes.DataUtils.NOTE_FONT_SIZE;
import static com.group.notes.DataUtils.NOTE_HIDE_BODY;
import static com.group.notes.DataUtils.NOTE_REQUEST_CODE;
import static com.group.notes.DataUtils.NOTE_TITLE;
import static com.group.notes.DataUtils.deleteNotes;
import static com.group.notes.DataUtils.isExternalStorageReadable;
import static com.group.notes.DataUtils.isExternalStorageWritable;
import static com.group.notes.DataUtils.retrieveData;
import static com.group.notes.DataUtils.saveData;


public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener,
        Toolbar.OnMenuItemClickListener, AbsListView.MultiChoiceModeListener,
        SearchView.OnQueryTextListener {

    private static File localPath, backupPath;


    private static ListView listView;
    private ImageButton newNote;
    private TextView noNotes;
    private Toolbar toolbar;
    private MenuItem searchMenu;

    private static JSONArray notes;
    private static NoteAdapter adapter;

    // массив записок выбранных на удаление
    public static ArrayList<Integer> checkedArray = new ArrayList<Integer>();
    public static boolean deleteActive = false;

    public static boolean searchActive = false;
    private ArrayList<Integer> realIndexesOfSearchResults;

    private int lastFirstVisibleItem = -1;
    private float newNoteButtonBaseYCoordinate;

    private AlertDialog backupCheckDialog, backupOKDialog, restoreCheckDialog, restoreFailedDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        localPath = new File(getFilesDir() + "/" + NOTES_FILE_NAME);

        File backupFolder = new File(Environment.getExternalStorageDirectory() +
                BACKUP_FOLDER_PATH);

        if (isExternalStorageReadable() && isExternalStorageWritable() && !backupFolder.exists())
            backupFolder.mkdir();

        backupPath = new File(backupFolder, BACKUP_FILE_NAME);

        /*
        if (Build.VERSION.SDK_INT >= 18)
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT);


        else
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
*/

        notes = new JSONArray();


        JSONArray tempNotes = retrieveData(localPath);


        if (tempNotes != null)
            notes = tempNotes;

        setContentView(R.layout.activity_main);

        toolbar = (Toolbar)findViewById(R.id.toolbarMain);
        listView = (ListView)findViewById(R.id.listView);
        newNote = (ImageButton)findViewById(R.id.newNote);
        noNotes = (TextView)findViewById(R.id.noNotes);

        if (toolbar != null)
            initToolbar();

        newNoteButtonBaseYCoordinate = newNote.getY();

        // инициализируем NoteAdapter массивом заметок
        adapter = new NoteAdapter(getApplicationContext(), notes);
        listView.setAdapter(adapter);

        //один большой гемор с пролистыванием заметок...
        //по большому счету ниче интересного здесь не происходит
        listView.setOnItemClickListener(this);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        listView.setMultiChoiceModeListener(this);
        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (lastFirstVisibleItem == -1)
                    lastFirstVisibleItem = view.getFirstVisiblePosition();

                if (view.getFirstVisiblePosition() > lastFirstVisibleItem)
                    newNoteButtonVisibility(false);

                else if (view.getFirstVisiblePosition() < lastFirstVisibleItem &&
                        !deleteActive && !searchActive) {

                    newNoteButtonVisibility(true);
                }

                lastFirstVisibleItem = view.getFirstVisiblePosition();
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                                 int totalItemCount) {}
        });


        // вот тут передача действию EditActivity при нажатии на "+"
        newNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, EditActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                intent.putExtra(NOTE_REQUEST_CODE, NEW_NOTE_REQUEST);

                startActivityForResult(intent, NEW_NOTE_REQUEST);
            }
        });

        // если есть заметки,то убираем надпись со стартового экрана
        if (notes.length() == 0)
            noNotes.setVisibility(View.VISIBLE);

        else
            noNotes.setVisibility(View.INVISIBLE);

        initDialogs(this);
    }

    //опять же инит Тулбара..ничего интересного
    protected void initToolbar() {
        toolbar.setTitle(R.string.app_name);

        toolbar.inflateMenu(R.menu.menu_main);

        toolbar.setOnMenuItemClickListener(this);

        Menu menu = toolbar.getMenu();

        if (menu != null) {
            searchMenu = menu.findItem(R.id.action_search);

            if (searchMenu != null) {
                SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchMenu);

                if (searchView != null) {
                    searchView.setQueryHint(getString(R.string.action_search));
                    searchView.setOnQueryTextListener(this);

                    MenuItemCompat.setOnActionExpandListener(searchMenu,
                            new MenuItemCompat.OnActionExpandListener() {

                        @Override
                        public boolean onMenuItemActionExpand(MenuItem item) {
                            searchActive = true;
                            newNoteButtonVisibility(false);
                            listView.setLongClickable(false);

                            realIndexesOfSearchResults = new ArrayList<Integer>();
                            for (int i = 0; i < notes.length(); i++)
                                realIndexesOfSearchResults.add(i);

                            adapter.notifyDataSetChanged();

                            return true;
                        }

                        @Override
                        public boolean onMenuItemActionCollapse(MenuItem item) {
                            searchEnded();
                            return true;
                        }
                    });
                }
            }
        }
    }

    //всякие разные диалоги: бэкапа, рестора и всякого разного.
    protected void initDialogs(Context context) {
        backupCheckDialog = new AlertDialog.Builder(context)
                .setTitle(R.string.action_backup)
                .setMessage(R.string.dialog_check_backup_if_sure)
                .setPositiveButton(R.string.yes_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (notes.length() > 0) {
                            boolean backupSuccessful = saveData(backupPath, notes);

                            if (backupSuccessful)
                                showBackupSuccessfulDialog();

                            else {
                                Toast toast = Toast.makeText(getApplicationContext(),
                                        getResources().getString(R.string.toast_backup_failed),
                                        Toast.LENGTH_SHORT);
                                toast.show();
                            }
                        }

                        else {
                            Toast toast = Toast.makeText(getApplicationContext(),
                                    getResources().getString(R.string.toast_backup_no_notes),
                                    Toast.LENGTH_SHORT);
                            toast.show();
                        }
                    }
                })
                .setNegativeButton(R.string.no_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create();

        //диалог на случай дуачного бэкапа
        backupOKDialog = new AlertDialog.Builder(context)
                .setTitle(R.string.dialog_backup_created_title)
                .setMessage(getString(R.string.dialog_backup_created) + " "
                        + backupPath.getAbsolutePath())
                .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create();

        restoreCheckDialog = new AlertDialog.Builder(context)
                .setTitle(R.string.action_restore)
                .setMessage(R.string.dialog_check_restore_if_sure)
                .setPositiveButton(R.string.yes_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        JSONArray tempNotes = retrieveData(backupPath);

                        if (tempNotes != null) {
                            boolean restoreSuccessful = saveData(localPath, tempNotes);

                            if (restoreSuccessful) {
                                notes = tempNotes;

                                adapter = new NoteAdapter(getApplicationContext(), notes);
                                listView.setAdapter(adapter);

                                Toast toast = Toast.makeText(getApplicationContext(),
                                        getResources().getString(R.string.toast_restore_successful),
                                        Toast.LENGTH_SHORT);
                                toast.show();

                                // If no notes -> show 'Press + to add new note' text, invisible otherwise
                                if (notes.length() == 0)
                                    noNotes.setVisibility(View.VISIBLE);

                                else
                                    noNotes.setVisibility(View.INVISIBLE);
                            }

                            else {
                                Toast toast = Toast.makeText(getApplicationContext(),
                                        getResources().getString(R.string.toast_restore_unsuccessful),
                                        Toast.LENGTH_SHORT);
                                toast.show();
                            }
                        }

                        else
                            showRestoreFailedDialog();
                    }
                })
                .setNegativeButton(R.string.no_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create();


        // рестор не удался, так как не найден файл бэкапа
        restoreFailedDialog = new AlertDialog.Builder(context)
                .setTitle(R.string.dialog_restore_failed_title)
                .setMessage(R.string.dialog_restore_failed)
                .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create();
    }

    protected void showBackupSuccessfulDialog() {
        backupCheckDialog.dismiss();
        backupOKDialog.show();
    }

    protected void showRestoreFailedDialog() {
        restoreCheckDialog.dismiss();
        restoreFailedDialog.show();
    }


    //опять, если тыкаем на заметку -> пердаем управление EditActivity
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent intent = new Intent(this, EditActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

        if (searchActive) {
            int newPosition = realIndexesOfSearchResults.get(position);

            try {
                intent.putExtra(NOTE_TITLE, notes.getJSONObject(newPosition).getString(NOTE_TITLE));
                intent.putExtra(NOTE_BODY, notes.getJSONObject(newPosition).getString(NOTE_BODY));
                intent.putExtra(NOTE_COLOUR, notes.getJSONObject(newPosition).getString(NOTE_COLOUR));
                intent.putExtra(NOTE_FONT_SIZE, notes.getJSONObject(newPosition).getInt(NOTE_FONT_SIZE));

                if (notes.getJSONObject(newPosition).has(NOTE_HIDE_BODY)) {
                    intent.putExtra(NOTE_HIDE_BODY,
                            notes.getJSONObject(newPosition).getBoolean(NOTE_HIDE_BODY));
                }

                else
                    intent.putExtra(NOTE_HIDE_BODY, false);

            } catch (JSONException e) {
                e.printStackTrace();
            }

            intent.putExtra(NOTE_REQUEST_CODE, newPosition);
            startActivityForResult(intent, newPosition);
        }

        else {
            try {
                intent.putExtra(NOTE_TITLE, notes.getJSONObject(position).getString(NOTE_TITLE));
                intent.putExtra(NOTE_BODY, notes.getJSONObject(position).getString(NOTE_BODY));
                intent.putExtra(NOTE_COLOUR, notes.getJSONObject(position).getString(NOTE_COLOUR));
                intent.putExtra(NOTE_FONT_SIZE, notes.getJSONObject(position).getInt(NOTE_FONT_SIZE));

                if (notes.getJSONObject(position).has(NOTE_HIDE_BODY)) {
                    intent.putExtra(NOTE_HIDE_BODY,
                            notes.getJSONObject(position).getBoolean(NOTE_HIDE_BODY));
                }

                else
                    intent.putExtra(NOTE_HIDE_BODY, false);

            } catch (JSONException e) {
                e.printStackTrace();
            }

            intent.putExtra(NOTE_REQUEST_CODE, position);
            startActivityForResult(intent, position);
        }
    }

    //обработка всяких нажатий в менюшке на главном экране
    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        int id = menuItem.getItemId();

        if (id == R.id.action_backup) {
            backupCheckDialog.show();
            return true;
        }

        if (id == R.id.action_restore) {
            restoreCheckDialog.show();
            return true;
        }
        return false;
    }


   //мульи выбор на удаление
    @Override
    public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
        if (checked)
            checkedArray.add(position);

        else {
            int index = -1;

            for (int i = 0; i < checkedArray.size(); i++) {
                if (position == checkedArray.get(i)) {
                    index = i;
                    break;
                }
            }


            if (index != -1)
                checkedArray.remove(index);
        }

        // штука которая будет писать скок выбрано на удаление
        mode.setTitle(checkedArray.size() + " " + getString(R.string.action_delete_selected_number));
        adapter.notifyDataSetChanged();
    }

    //что происходит если нажать на иконку мусорки при удалении
    @Override
    public boolean onActionItemClicked(final ActionMode mode, MenuItem item) {
        //снова AlertDialog....в прочем ничего нового
        if (item.getItemId() == R.id.action_delete) {
            new AlertDialog.Builder(this)
                    .setMessage(R.string.dialog_delete)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            notes = deleteNotes(notes, checkedArray);

                            // единственное,будет создавать новый listadapter, при ответе ДА
                            adapter = new NoteAdapter(getApplicationContext(), notes);
                            listView.setAdapter(adapter);

                            Boolean saveSuccessful = saveData(localPath, notes);

                            // если все удачненько,выведем,что удалено успешно
                            if (saveSuccessful) {
                                Toast toast = Toast.makeText(getApplicationContext(),
                                        getResources().getString(R.string.toast_deleted),
                                        Toast.LENGTH_SHORT);
                                toast.show();
                            }


                            if (notes.length() == 0)
                                noNotes.setVisibility(View.VISIBLE);

                            else
                                noNotes.setVisibility(View.INVISIBLE);

                            mode.finish();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .show();

            return true;
        }

        return false;
    }

    // при долгом нажатии стратует режим выбора-удаления
    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mode.getMenuInflater().inflate(R.menu.menu_delete, menu);
        deleteActive = true;
        newNoteButtonVisibility(false);
        adapter.notifyDataSetChanged();
        return true;
    }

    // когда режим выбор-удаление заканчивается
    @Override
    public void onDestroyActionMode(ActionMode mode) {
        checkedArray = new ArrayList<Integer>();
        deleteActive = false;
        newNoteButtonVisibility(true);
        adapter.notifyDataSetChanged();
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }


    protected void newNoteButtonVisibility(boolean isVisible) {
        if (isVisible) {
            newNote.animate().cancel();
            newNote.animate().translationY(newNoteButtonBaseYCoordinate);
        } else {
            newNote.animate().cancel();
            newNote.animate().translationY(newNoteButtonBaseYCoordinate + 500);
        }
    }


    //обработка виджета ПОИСК
    @Override
    public boolean onQueryTextChange(String s) {
        s = s.toLowerCase();


        if (s.length() > 0) {
            JSONArray notesFound = new JSONArray();
            realIndexesOfSearchResults = new ArrayList<Integer>();

            // петля для заметок
            for (int i = 0; i < notes.length(); i++) {
                JSONObject note = null;

                try {
                    note = notes.getJSONObject(i);

                } catch (JSONException e) {
                    e.printStackTrace();
                }


                if (note != null) {
                    try {
                        if (note.getString(NOTE_TITLE).toLowerCase().contains(s) ||
                            note.getString(NOTE_BODY).toLowerCase().contains(s)) {

                            notesFound.put(note);
                            realIndexesOfSearchResults.add(i);
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }

            NoteAdapter searchAdapter = new NoteAdapter(getApplicationContext(), notesFound);
            listView.setAdapter(searchAdapter);
        }


        else {
            realIndexesOfSearchResults = new ArrayList<Integer>();
            for (int i = 0; i < notes.length(); i++)
                realIndexesOfSearchResults.add(i);

            adapter = new NoteAdapter(getApplicationContext(), notes);
            listView.setAdapter(adapter);
        }

        return false;
    }

    @Override
    public boolean onQueryTextSubmit(String s) {
        return false;
    }

    //что делать когда поиск заканчивается
    protected void searchEnded() {
        searchActive = false;
        adapter = new NoteAdapter(getApplicationContext(), notes);
        listView.setAdapter(adapter);
        listView.setLongClickable(true);
        newNoteButtonVisibility(true);
    }


    //возврат от EditActivity к MainActivity
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (searchActive && searchMenu != null)
                searchMenu.collapseActionView();

            Bundle mBundle = null;
            if (data != null)
                mBundle = data.getExtras();

            if (mBundle != null) {
                // если замтека была сохранена
                if (requestCode == NEW_NOTE_REQUEST) {
                    JSONObject newNoteObject = null;

                    try {
                        // добавляем в массив новую заметку
                        newNoteObject = new JSONObject();
                        newNoteObject.put(NOTE_TITLE, mBundle.getString(NOTE_TITLE));
                        newNoteObject.put(NOTE_BODY, mBundle.getString(NOTE_BODY));
                        newNoteObject.put(NOTE_COLOUR, mBundle.getString(NOTE_COLOUR));
                        newNoteObject.put(NOTE_FAVOURED, false);
                        newNoteObject.put(NOTE_FONT_SIZE, mBundle.getInt(NOTE_FONT_SIZE));
                        newNoteObject.put(NOTE_HIDE_BODY, mBundle.getBoolean(NOTE_HIDE_BODY));

                        notes.put(newNoteObject);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }


                    if (newNoteObject != null) {
                        adapter.notifyDataSetChanged();

                        Boolean saveSuccessful = saveData(localPath, notes);

                        if (saveSuccessful) {
                            Toast toast = Toast.makeText(getApplicationContext(),
                                    getResources().getString(R.string.toast_new_note),
                                    Toast.LENGTH_SHORT);
                            toast.show();
                        }

                        if (notes.length() == 0)
                            noNotes.setVisibility(View.VISIBLE);

                        else
                            noNotes.setVisibility(View.INVISIBLE);
                    }
                }

                // если существующая заметка была просто отредактировнаа
                else {
                    JSONObject newNoteObject = null;

                    try {
                        newNoteObject = notes.getJSONObject(requestCode);
                        newNoteObject.put(NOTE_TITLE, mBundle.getString(NOTE_TITLE));
                        newNoteObject.put(NOTE_BODY, mBundle.getString(NOTE_BODY));
                        newNoteObject.put(NOTE_COLOUR, mBundle.getString(NOTE_COLOUR));
                        newNoteObject.put(NOTE_FONT_SIZE, mBundle.getInt(NOTE_FONT_SIZE));
                        newNoteObject.put(NOTE_HIDE_BODY, mBundle.getBoolean(NOTE_HIDE_BODY));

                        notes.put(requestCode, newNoteObject);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }


                    if (newNoteObject != null) {
                        adapter.notifyDataSetChanged();

                        Boolean saveSuccessful = saveData(localPath, notes);

                        if (saveSuccessful) {
                            Toast toast = Toast.makeText(getApplicationContext(),
                                    getResources().getString(R.string.toast_note_saved),
                                    Toast.LENGTH_SHORT);
                            toast.show();
                        }
                    }
                }
            }
        }

        //что если просто вышли ничего не сохранив
        else if (resultCode == RESULT_CANCELED) {
            Bundle mBundle = null;


            if (data != null && data.hasExtra("request") && requestCode == NEW_NOTE_REQUEST) {
                mBundle = data.getExtras();

            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }



    public static void setFavourite(Context context, boolean favourite, int position) {
        JSONObject newFavourite = null;

        try {
            newFavourite = notes.getJSONObject(position);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (newFavourite != null) {
            if (favourite) {
                // Set favoured to true
                try {
                    newFavourite.put(NOTE_FAVOURED, true);

                } catch (JSONException e) {
                    e.printStackTrace();
                }


                // сортируем если Избранная заметка не первая
                if (position > 0) {
                    JSONArray newArray = new JSONArray();

                    try {
                        newArray.put(0, newFavourite);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    for (int i = 0; i < notes.length(); i++) {
                        if (i != position) {
                            try {
                                newArray.put(notes.get(i));

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    notes = newArray;
                    adapter = new NoteAdapter(context, notes);
                    listView.setAdapter(adapter);

                    // Smooth scroll to top
                    listView.post(new Runnable() {
                        public void run() {
                            listView.smoothScrollToPosition(0);
                        }
                    });
                }

                // если Избранная заметка и так была первая, то просто нотифай адаптер
                else {
                    try {
                        notes.put(position, newFavourite);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    adapter.notifyDataSetChanged();
                }
            }

            else {
                try {
                    newFavourite.put(NOTE_FAVOURED, false);
                    notes.put(position, newFavourite);

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                adapter.notifyDataSetChanged();
            }


            saveData(localPath, notes);
        }
    }


    //что если нажать back когда идет поиск
    @Override
    public void onBackPressed() {
        if (searchActive && searchMenu != null) {
            searchMenu.collapseActionView();
            return;
        }

        super.onBackPressed();
    }



    public static File getLocalPath() {
        return localPath;
    }


    public static File getBackupPath() {
        return backupPath;
    }
}
