package com.habitrpg.android.habitica.ui.fragments.inventory.items;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.habitrpg.android.habitica.R;
import com.habitrpg.android.habitica.components.AppComponent;
import com.habitrpg.android.habitica.data.InventoryRepository;
import com.habitrpg.android.habitica.events.ContentReloadedEvent;
import com.habitrpg.android.habitica.events.OpenedMysteryItemEvent;
import com.habitrpg.android.habitica.events.commands.OpenMenuItemCommand;
import com.habitrpg.android.habitica.helpers.ReactiveErrorHandler;
import com.habitrpg.android.habitica.models.inventory.Item;
import com.habitrpg.android.habitica.models.inventory.Pet;
import com.habitrpg.android.habitica.models.inventory.SpecialItem;
import com.habitrpg.android.habitica.models.user.User;
import com.habitrpg.android.habitica.ui.activities.MainActivity;
import com.habitrpg.android.habitica.ui.adapter.inventory.ItemRecyclerAdapter;
import com.habitrpg.android.habitica.ui.fragments.BaseFragment;
import com.habitrpg.android.habitica.ui.helpers.RecyclerViewEmptySupport;
import com.habitrpg.android.habitica.ui.helpers.UiUtils;
import com.habitrpg.android.habitica.ui.menu.DividerItemDecoration;
import com.habitrpg.android.habitica.ui.menu.MainDrawerBuilder;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.HashMap;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.realm.OrderedRealmCollection;
import io.realm.RealmList;

import static com.habitrpg.android.habitica.ui.helpers.UiUtils.showSnackbar;

public class ItemRecyclerFragment extends BaseFragment {

    @Inject
    InventoryRepository inventoryRepository;

    private static final String ITEM_TYPE_KEY = "CLASS_TYPE_KEY";
    @BindView(R.id.recyclerView)
    public RecyclerViewEmptySupport recyclerView;
    @BindView(R.id.empty_view)
    public View emptyView;
    @BindView(R.id.empty_text_view)
    public TextView emptyTextView;
    @BindView(R.id.titleTextView)
    public TextView titleView;
    @BindView(R.id.footerTextView)
    public TextView footerView;
    @BindView(R.id.openMarketButton)
    public Button openMarketButton;
    public ItemRecyclerAdapter adapter;
    public String itemType;
    public String itemTypeText;
    public Boolean isHatching;
    public Boolean isFeeding;
    public Item hatchingItem;
    public Pet feedingPet;
    @Nullable
    public User user;
    LinearLayoutManager layoutManager = null;

    private View view;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        if (view == null) {
            view = inflater.inflate(R.layout.fragment_items, container, false);
        }
        unbinder = ButterKnife.bind(this, view);

        recyclerView.setEmptyView(emptyView);
        emptyTextView.setText(getString(R.string.empty_items, itemTypeText));

        android.support.v4.app.FragmentActivity context = getActivity();

        layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();

        if (layoutManager == null) {
            layoutManager = new LinearLayoutManager(context);

            recyclerView.setLayoutManager(layoutManager);
        }

        adapter = (ItemRecyclerAdapter) recyclerView.getAdapter();
        if (adapter == null) {
            adapter = new ItemRecyclerAdapter(null, true);
            adapter.context = this.getActivity();
            adapter.isHatching = this.isHatching;
            adapter.isFeeding = this.isFeeding;
            adapter.fragment = this;
            if (this.hatchingItem != null) {
                adapter.hatchingItem = this.hatchingItem;
            }
            if (this.feedingPet != null) {
                adapter.feedingPet = this.feedingPet;
            }
            recyclerView.setAdapter(adapter);

            compositeSubscription.add(adapter.getSellItemEvents()
                    .flatMap(item -> inventoryRepository.sellItem(user, item))
                    .subscribe(item -> {}, ReactiveErrorHandler.handleEmptyError()));

            compositeSubscription.add(adapter.getQuestInvitationEvents()
                    .flatMap(quest -> inventoryRepository.inviteToQuest(quest))
                            .subscribe(group -> {
                                OpenMenuItemCommand event1 = new OpenMenuItemCommand();
                                event1.identifier = MainDrawerBuilder.SIDEBAR_PARTY;
                                EventBus.getDefault().post(event1);
                            }, throwable -> {}));
        }
        recyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST));

        if (savedInstanceState != null) {
            this.itemType = savedInstanceState.getString(ITEM_TYPE_KEY, "");
        }

        if (this.isHatching != null && this.isHatching) {
            getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
            this.titleView.setText(getString(R.string.hatch_with, this.hatchingItem.getText()));
            this.titleView.setVisibility(View.VISIBLE);
            this.footerView.setText(getString(R.string.hatching_market_info));
            this.footerView.setVisibility(View.VISIBLE);
            this.openMarketButton.setVisibility(View.VISIBLE);
        } else if (this.isFeeding != null && this.isFeeding) {
            getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
            this.titleView.setText(getString(R.string.dialog_feeding, this.feedingPet.getColorText(), this.feedingPet.getAnimalText()));
            this.titleView.setVisibility(View.VISIBLE);
            this.footerView.setText(getString(R.string.feeding_market_info));
            this.footerView.setVisibility(View.VISIBLE);
            this.openMarketButton.setVisibility(View.VISIBLE);
        } else {
            this.titleView.setVisibility(View.GONE);
            this.footerView.setVisibility(View.GONE);
            this.openMarketButton.setVisibility(View.GONE);
        }

        return view;
    }

    @Override
    public void onDestroy() {
        inventoryRepository.close();
        super.onDestroy();
    }

    @Override
    public void injectFragment(AppComponent component) {
        component.inject(this);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.loadItems();
    }

    @Override
    public void onResume() {
        if (this.isHatching != null && this.isHatching && getDialog().getWindow() != null) {
            ViewGroup.LayoutParams params = getDialog().getWindow().getAttributes();
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            params.height = ViewGroup.LayoutParams.MATCH_PARENT;
            getDialog().getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);
        }

        super.onResume();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ITEM_TYPE_KEY, this.itemType);
    }

    private void loadItems() {
        inventoryRepository.getOwnedItems(itemType).first().subscribe(items -> {
            if (this.itemType.equals("special")) {
                if (user != null && user.getPurchased() != null && user.getPurchased().getPlan().isActive()) {
                    Item mysterItem = SpecialItem.makeMysteryItem(getContext());
                    mysterItem.setOwned(user.getPurchased().getPlan().mysteryItems.size());
                }
            }

            if (items.size() > 0) {
                adapter.updateData((OrderedRealmCollection<Item>) items);
            }
        }, ReactiveErrorHandler.handleEmptyError());

        compositeSubscription.add(inventoryRepository.getOwnedPets().subscribe(adapter::setOwnedPets, ReactiveErrorHandler.handleEmptyError()));
    }

    @OnClick(R.id.openMarketButton)
    public void onOpenMarketClicked() {
        dismiss();
        openMarket();
    }

    @OnClick(R.id.openEmptyMarketButton)
    public void onEmptyOpenMarketButtonClicked() {
        openMarket();
    }

    private void openMarket() {
        OpenMenuItemCommand command = new OpenMenuItemCommand();
        command.identifier = MainDrawerBuilder.SIDEBAR_SHOPS;
        EventBus.getDefault().post(command);
    }

    @Subscribe
    public void openedMysteryItem(OpenedMysteryItemEvent event) {
        this.adapter.openedMysteryItem(event.numberLeft);
        inventoryRepository.getEquipment(event.mysteryItem.getKey()).subscribe(itemData -> {
            MainActivity activity = (MainActivity) getActivity();
            showSnackbar(activity, activity.floatingMenuWrapper, getString(R.string.notification_mystery_item, itemData.getText()), UiUtils.SnackbarDisplayType.NORMAL);
        });
    }
}
