package com.group.notes;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static com.group.notes.DataUtils.*;
import static com.group.notes.MainActivity.*;


/*
  Грубо говоря,класс ответсвенен за то, чтобы представлять список заметок,которые созданы
 */
class NoteAdapter extends BaseAdapter implements ListAdapter {
    private Context context;
    private JSONArray adapterData;
    private LayoutInflater inflater;

    //так как BaseAdapter (дефолтных методов в нем нет) интерфейс, то нужно переопределить все его методы

   //конструктор...адаптерам нужно писать конструктеры
    NoteAdapter(Context context, JSONArray adapterData) {
        this.context = context;
        this.adapterData = adapterData;
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    // тут просто, возвращает количество заметок
    @Override
    public int getCount() {
        if (this.adapterData != null) //если JSONовский массив не пустой,то вернем его длину
            return this.adapterData.length();
        else
            return 0;
    }

    // вернем JSON объект если есть по заданной позиции
    //по идее,если его нет ,то метод и так вернет null, но хер знает,не тестил,поэтому оставил else
    @Override
    public JSONObject getItem(int position) {
        if (this.adapterData != null)
            return this.adapterData.optJSONObject(position);

        else
            return null;
    }

    //это функция нахрен не нужна, но реализовать надо, поэтому держите возвращаемый 0
    @Override
    public long getItemId(int position) {
        return 0;
    }


    // пункт списка
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        if (convertView == null)
            convertView = this.inflater.inflate(R.layout.list_view_note, parent, false);

        // Андроид во всей красе. Каждый объект на макете нужно сначала найти в файле R
        //а затем явно преобразовать в заданный тип
        RelativeLayout relativeLayout = (RelativeLayout) convertView.findViewById(R.id.relativeLayout);
        LayerDrawable roundedCard = (LayerDrawable) context.getResources().getDrawable(R.drawable.rounded_card);
        TextView titleView = (TextView) convertView.findViewById(R.id.titleView);
        TextView bodyView = (TextView) convertView.findViewById(R.id.bodyView);
        ImageButton favourite = (ImageButton) convertView.findViewById(R.id.favourite);

        JSONObject noteObject = getItem(position);

        //работаем, если JSONObject взятый предыдущим методом не null
        if (noteObject != null) {
            // ну раз не пустой, то синициализируем переменные
            String title = context.getString(R.string.note_title);//название
            String body = context.getString(R.string.note_body);//сама заметка
            String colour = String.valueOf(context.getResources().getColor(R.color.white));//цвет фона
            int fontSize = 18;
            Boolean hideBody = false;
            Boolean favoured = false;

            //кароч геттеры требуют обязательной обработки ошибок...да, в Джаве такое есть
            try {

                title = noteObject.getString(NOTE_TITLE);
                body = noteObject.getString(NOTE_BODY);
                colour = noteObject.getString(NOTE_COLOUR);


                if (noteObject.has(NOTE_HIDE_BODY))
                    hideBody = noteObject.getBoolean(NOTE_HIDE_BODY);

                favoured = noteObject.getBoolean(NOTE_FAVOURED);


                //окей,пусть будет StackTrace, но это так тупо смотреть логи стэктрейса....
            } catch (JSONException e) {
                //e.printStackTrace();
            }

            // тут смотрим является избранным или нет
            if (favoured)
                //тут собственно назначаем ресурс отображения
                favourite.setImageResource(R.drawable.ic_fav);

            else
                favourite.setImageResource(R.drawable.ic_unfav);


            titleView.setText(title);


            if (hideBody)
                bodyView.setVisibility(View.GONE);


            // подсветим заметку, если она выбрана на удаление
            if (checkedArray.contains(position)) {
                ((GradientDrawable) roundedCard.findDrawableByLayerId(R.id.card))
                        .setColor(context.getResources().getColor(R.color.theme_primary));
            }

            // а если не выбрана, то оставляем цвет дефолтным
            else {
                ((GradientDrawable) roundedCard.findDrawableByLayerId(R.id.card))
                        .setColor(Color.parseColor(colour));
            }

            relativeLayout.setBackground(roundedCard);

            final Boolean finalFavoured = favoured;
            favourite.setOnClickListener(new View.OnClickListener() {
                // если происходит нажатие, то меняем статус с избранного на не-избранное или наоборот
                @Override
                public void onClick(View v) {
                    setFavourite(context, !finalFavoured, position);
                }
            });
        }

        return convertView;
    }
}
