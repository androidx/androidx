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
 */

package android.support.v17.leanback.supportleanbackshowcase.cards.presenters;

import android.content.Context;
import android.support.v17.leanback.supportleanbackshowcase.cards.CharacterCardView;
import android.support.v17.leanback.supportleanbackshowcase.models.Card;

/**
 * This Presenter is used to display the characters card row in the DetailView examples.
 */
public class CharacterCardPresenter extends AbstractCardPresenter<CharacterCardView> {

    public CharacterCardPresenter(Context context) {
        super(context);
    }

    @Override
    protected CharacterCardView onCreateView() {
        return new CharacterCardView(getContext());
    }

    @Override
    public void onBindViewHolder(Card card, CharacterCardView cardView) {
        cardView.updateUi(card);
    }

}
