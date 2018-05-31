/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.androidx.car;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.car.widget.AlphaJumpBucketer;
import androidx.car.widget.IAlphaJumpAdapter;
import androidx.car.widget.PagedListView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Arrays;
import java.util.Collection;

/**
 * An activity with a long list of cheeses, initially in a random order but you can use alpha jump
 * to quickly jump to your favourite cheese.
 */
public class AlphaJumpActivity extends Activity {
    private static final String TAG = "AlphaJumpActivity";

    private static final String[] CHEESES = {
            "Pourly", "Macconais", "Bonchester", "Olivet Cendre", "Fruit Cream Cheese",
            "Metton (Cancoillotte)", "Lyonnais", "Crema Agria", "Nantais",
            "Brusselae Kaas (Fromage de Bruxelles)", "Rouleau De Beaulieu", "Flor de Guia",
            "Poivre d'Ane", "Tomme des Chouans", "Whitestone Farmhouse", "Queso de Murcia",
            "Saint-Marcellin", "Pave d'Affinois", "Quatre-Vents", "Galette du Paludier", "Pyramide",
            "Capricorn Goat", "Feta", "Queso del Montsec", "Telemea", "Cooleney",
            "Buchette d'Anjou", "Banon", "Bosworth", "Bergader", "Mothais a la Feuille",
            "Mascarpone Torta", "Richelieu", "Guerbigny", "Taupiniere", "Anneau du Vic-Bilh",
            "Tupi", "Queso Fresco", "Timboon Brie", "Neufchatel", "Blue Castello",
            "Brebis du Puyfaucon", "Gratte-Paille", "Palet de Babligny", "Caciotta", "Rigotte",
            "Caciocavallo", "Bleu Des Causses", "Civray", "Bath Cheese", "Farmer", "Cachaille",
            "Ricotta", "Caravane", "Selles sur Cher", "Chaource", "Cottage Cheese (Australian)",
            "Pelardon des Corbieres", "Cold Pack", "Queso Fresco (Adobera)", "Bleu de Gex",
            "Provel", "Torta del Casar", "Golden Cross", "Mascarpone", "Fougerus",
            "Dessertnyj Belyj", "Fresh Ricotta", "Gris de Lille", "Breakfast Cheese", "Venaco",
            "Pant ys Gawn", "Mascarpone (Australian)", "Sharpam", "Humboldt Fog",
            "Evansdale Farmhouse Brie", "Kernhem", "Mozzarella Rolls", "Dolcelatte",
            "Briquette de Brebis", "Niolo", "Selva", "Dunbarra", "King Island Cape Wickham Brie",
            "Carre de l'Est", "Broccio", "Castelo Branco", "Finn", "Panela", "Basket Cheese",
            "Woodside Cabecou", "Truffe", "Flower Marie", "Mozzarella Fresh, in water",
            "Jubilee Blue", "Coeur de Camembert au Calvados", "Caprice des Dieux", "Caboc",
            "Crottin du Chavignol", "Cabecou", "Cottage Cheese", "Cashel Blue", "Patefine Fort",
            "Mahoe Aged Gouda", "Cornish Pepper", "Greuilh", "Ricotta (Australian)", "Grand Vatel",
            "Prince-Jean", "Coulommiers", "Scamorza", "Romans Part Dieu", "Quark", "Frinault",
            "Chabichou du Poitou", "Le Lacandou", "Maredsous", "Fin-de-Siecle", "Button (Innes)",
            "Washed Rind Cheese (Australian)", "Daralagjazsky", "Margotin", "Pithtviers au Foin",
            "Cathelain", "Yarra Valley Pyramid", "Sirene", "Emlett", "Explorateur", "Bandal",
            "Lingot Saint Bousquet d'Orb", "Gorgonzola", "Bresse Bleu", "Beer Cheese",
            "Brinza (Burduf Brinza)", "Bakers", "Little Rydings", "Bryndza"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paged_list_view);

        PagedListView pagedListView = findViewById(R.id.paged_list_view);
        pagedListView.setAdapter(new CheeseAdapter());
    }

    /**
     * Adapter that populates a number of items for demo purposes.
     */
    public static class CheeseAdapter extends RecyclerView.Adapter<CheeseAdapter.ViewHolder>
            implements IAlphaJumpAdapter {
        private String[] mCheeses;
        private boolean mIsSorted;

        CheeseAdapter() {
            // Start out not being sorted.
            mCheeses = CHEESES;
            mIsSorted = false;
        }

        @Override
        public CheeseAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            View view = inflater.inflate(R.layout.alpha_jump_list_item, parent, false);
            return new CheeseAdapter.ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(CheeseAdapter.ViewHolder holder, int position) {
            holder.mTextView.setText(mCheeses[position]);
        }

        @Override
        public int getItemCount() {
            return mCheeses.length;
        }

        @Override
        public Collection<Bucket> getAlphaJumpBuckets() {
            if (!mIsSorted) {
                Log.i(TAG, "Sorting...");
                // We'll sort the first time we need to populate the buckets.
                mCheeses = mCheeses.clone();
                Arrays.sort(mCheeses);
                mIsSorted = true;
                notifyDataSetChanged();
            }

            AlphaJumpBucketer bucketer = new AlphaJumpBucketer();
            return bucketer.createBuckets(mCheeses);
        }

        @Override
        public void onAlphaJumpEnter() {
            Log.i(TAG, "onAlphaJumpEnter");
        }

        @Override
        public void onAlphaJumpLeave(Bucket bucket) {
            Log.i(TAG, "onAlphaJumpLeave: " + bucket.getLabel());
        }

        /**
         * ViewHolder for CheeseAdapter.
         */
        public static class ViewHolder extends RecyclerView.ViewHolder {
            private final TextView mTextView;

            public ViewHolder(View itemView) {
                super(itemView);
                mTextView = itemView.findViewById(R.id.text);
            }
        }
    }
}
