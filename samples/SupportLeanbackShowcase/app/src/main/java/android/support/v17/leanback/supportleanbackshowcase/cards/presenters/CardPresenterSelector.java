/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package android.support.v17.leanback.supportleanbackshowcase.cards.presenters;

import android.content.Context;
import android.support.v17.leanback.supportleanbackshowcase.R;
import android.support.v17.leanback.supportleanbackshowcase.models.Card;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.PresenterSelector;

import java.util.HashMap;

/**
 * This PresenterSelector will decide what Presenter to use depending on a given card's type.
 */
public class CardPresenterSelector extends PresenterSelector {

    private final Context mContext;
    private final HashMap<Card.Type, Presenter> presenters = new HashMap<Card.Type, Presenter>();

    public CardPresenterSelector(Context context) {
        mContext = context;
    }

    @Override
    public Presenter getPresenter(Object item) {
        if (!(item instanceof Card)) throw new RuntimeException(
                String.format("The PresenterSelector only supports data items of type '%s'",
                        Card.class.getName()));
        Card card = (Card) item;
        Presenter presenter = presenters.get(card.getType());
        if (presenter == null) {
            switch (card.getType()) {
                case SINGLE_LINE:
                    presenter = new SingleLineCardPresenter(mContext);
                    break;
                case MOVIE:
                case MOVIE_BASE:
                case MOVIE_COMPLETE:
                case SQUARE_BIG:
                case GRID_SQUARE:
                case GAME: {
                    int themeResId = R.style.MovieCardSimpleTheme;
                    if (card.getType() == Card.Type.MOVIE_BASE) {
                        themeResId = R.style.MovieCardBasicTheme;
                    } else if (card.getType() == Card.Type.MOVIE_COMPLETE) {
                        themeResId = R.style.MovieCardCompleteTheme;
                    } else if (card.getType() == Card.Type.SQUARE_BIG) {
                        themeResId = R.style.SquareBigCardTheme;
                    } else if (card.getType() == Card.Type.GRID_SQUARE) {
                        themeResId = R.style.GridCardTheme;
                    } else if (card.getType() == Card.Type.GAME) {
                        themeResId = R.style.GameCardTheme;
                    }
                    presenter = new ImageCardViewPresenter(mContext, themeResId);
                    break;
                }
                case SIDE_INFO:
                    presenter = new SideInfoCardPresenter(mContext);
                    break;
                case TEXT:
                    presenter = new TextCardPresenter(mContext);
                    break;
                case ICON:
                    presenter = new IconCardPresenter(mContext);
                    break;
                case CHARACTER:
                    presenter = new CharacterCardPresenter(mContext);
                    break;
                default:
                    presenter = new ImageCardViewPresenter(mContext);
                    break;
            }
        }
        presenters.put(card.getType(), presenter);
        return presenter;
    }

}
