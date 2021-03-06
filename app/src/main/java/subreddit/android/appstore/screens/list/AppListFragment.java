package subreddit.android.appstore.screens.list;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import subreddit.android.appstore.AppStoreApp;
import subreddit.android.appstore.R;
import subreddit.android.appstore.backend.AppInfo;
import subreddit.android.appstore.screens.details.AboutActivity;
import subreddit.android.appstore.screens.details.AppDetailsActivity;
import subreddit.android.appstore.util.mvp.BasePresenterFragment;
import subreddit.android.appstore.util.mvp.PresenterFactory;
import subreddit.android.appstore.util.ui.BaseViewHolder;
import subreddit.android.appstore.util.ui.DividerItemDecoration;


public class AppListFragment extends BasePresenterFragment<AppListContract.Presenter, AppListContract.View>
        implements AppListContract.View, BaseViewHolder.ClickListener, FilterListAdapter.FilterListener {

    @BindView(R.id.list_appinfos) RecyclerView appList;
    @BindView(R.id.applist_container) View applistContainer;
    @BindView(R.id.loading_container) View loadingContainer;
    @BindView(R.id.drawerlayout) DrawerLayout drawerLayout;
    @BindView(R.id.list_tagfilter) RecyclerView filterList;

    @Inject
    PresenterFactory<AppListContract.Presenter> presenterFactory;

    SearchView searchView;
    Unbinder unbinder;
    AppListAdapter appListAdapter;
    FilterListAdapter filterListAdapter;

    public static AppListFragment newInstance() {
        return new AppListFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        DaggerAppListComponent.builder()
                .appComponent(AppStoreApp.Injector.INSTANCE.getAppComponent())
                .build().inject(this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_applist_layout, container, false);
        unbinder = ButterKnife.bind(this, layout);
        return layout;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        appList.setLayoutManager(new LinearLayoutManager(getContext()));
        appList.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL_LIST));
        appListAdapter = new AppListAdapter();
        appListAdapter.setItemClickListener(this);
        appList.setAdapter(appListAdapter);

        filterList.setLayoutManager(new LinearLayoutManager(getContext()));
        filterList.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL_LIST));
        filterListAdapter = new FilterListAdapter(this);
        filterList.setAdapter(filterListAdapter);
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void updateTagCount(int[] tagCount) {
        filterListAdapter.updateTagCount(tagCount);
        filterListAdapter.notifyDataSetChanged();
    }

    @Override
    public void onPresenterReady(@NonNull AppListContract.Presenter presenter) {
        super.onPresenterReady(presenter);
    }

    @NonNull
    @Override
    protected PresenterFactory<AppListContract.Presenter> getPresenterFactory() {
        return presenterFactory;
    }

    @Override
    public void onDestroyView() {
        if (unbinder != null) unbinder.unbind();
        super.onDestroyView();
    }

    @Override
    public void showAppList(List<AppInfo> appInfos) {
        appListAdapter.setData(appInfos);
        appListAdapter.notifyDataSetChanged();
        applistContainer.setVisibility(View.VISIBLE);
        loadingContainer.setVisibility(View.GONE);
        setHasOptionsMenu(true);
    }

    @Override
    public void showLoadingScreen() {
        applistContainer.setVisibility(View.GONE);
        loadingContainer.setVisibility(View.VISIBLE);
        setHasOptionsMenu(false);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.applist_fragment, menu);
        final MenuItem searchItem = menu.findItem(R.id.menu_search);
        searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setQueryHint(getString(R.string.hint_type_to_filter));
        searchView.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                appListAdapter.getFilter().filter(query);
                if (searchView.isIconified()) searchView.setIconified(false);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                appListAdapter.getFilter().filter(s);
                return true;
            }
        });
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_force_refresh:
                getPresenter().refreshData();
                return true;
            case R.id.menu_search:
                return true;
            case R.id.menu_filter:
                toggleTagFilterDrawer();
                return true;
            case R.id.menu_about:
                startActivity(new Intent(getActivity(), AboutActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onItemClick(View view, int position, long itemId) {
        Intent intent = new Intent(getActivity(), AppDetailsActivity.class);
        intent.putExtra(AppDetailsActivity.ARG_KEY, appListAdapter.getItem(position));
        startActivity(intent);
        return true;
    }

    private boolean isTagFilterDrawerOpen() {
        return drawerLayout.isDrawerOpen(GravityCompat.END);
    }

    private void toggleTagFilterDrawer() {
        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END);
        } else {
            drawerLayout.openDrawer(GravityCompat.END);
        }
    }

    @Override
    public void onNewFilterTags(Collection<AppInfo.Tag> tags) {
        appListAdapter.getFilter().setFilterTags(tags);
        appListAdapter.getFilter().filter(appListAdapter.getFilter().getFilterString());
    }
}
