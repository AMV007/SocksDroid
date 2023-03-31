package net.typeblog.socks;

import android.Manifest;
import android.app.ListFragment;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import net.typeblog.socks.util.Profile;
import net.typeblog.socks.util.ProfileManager;

import java.util.Arrays;
import java.util.Set;

public class AppListFragment extends ListFragment {

    public static class AppPackage {
        public PackageInfo info;
        public boolean selected;
        public String label;

        public AppPackage(PackageInfo info, boolean selected, String label) {
            this.info = info;
            this.selected = selected;
            this.label = label;
        }
    }

    static class ViewHolder {
        ImageView appIcon;
        TextView appName;
        TextView appPkg;
        CheckBox checkBox;
    }

    public class AppListAdapter extends ArrayAdapter<AppPackage> {
        private final PackageManager pm;

        public AppListAdapter(Context context) {
            super(context, R.layout.app_item);
            pm = context.getPackageManager();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final ViewHolder viewHolder;
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.app_item, parent, false);
                viewHolder = new ViewHolder();
                viewHolder.appIcon = convertView.findViewById(R.id.icon);
                viewHolder.appName = convertView.findViewById(R.id.name);
                viewHolder.appPkg = convertView.findViewById(R.id.pkgName);
                viewHolder.checkBox = convertView.findViewById(R.id.checked);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            AppPackage pkg = getItem(position);
            ApplicationInfo appinfo = pkg.info.applicationInfo;
            viewHolder.appIcon.setImageDrawable(appinfo.loadIcon(pm));
            viewHolder.appName.setText(pkg.label);
            viewHolder.appPkg.setText(appinfo.packageName);
            viewHolder.checkBox.setChecked(pkg.selected);
            return convertView;
        }
    }


    public PackageManager pm;
    private boolean isChanged = false;
    private ProfileManager mManager;
    private Profile mProfile;
    private Set<String> apps;

    private AppListAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pm = getActivity().getPackageManager();
        mManager = new ProfileManager(getActivity().getApplicationContext());
        apps = getmProfile().getAppSets();
        adapter = new AppListAdapter(getActivity());

        initData();


    }

    public void initData() {
        for (PackageInfo info : pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)) {
            if (info.packageName.equals(getActivity().getPackageName())) {
                //Ignore Self
                continue;
            } else if (info.requestedPermissions == null) {
                //Ignore apps that do not have permission
                continue;
            } else if (!Arrays.asList(info.requestedPermissions).contains(Manifest.permission.INTERNET)) {
                //Ignore apps that do not have Internet permission
                continue;
            }
            boolean selected = apps.contains(info.packageName);
            String label = info.applicationInfo.loadLabel(pm).toString();
            AppPackage pkg = new AppPackage(info, selected, label);
            adapter.add(pkg);
        }
        //The selected apps is displayed first
        adapter.sort((a, b) -> {
            if (a.selected != b.selected)
                return a.selected ? -1 : 1;
            return a.label.compareTo(b.label);
        });

    }

    private Profile getmProfile() {
        if (mProfile == null) {
            mProfile = mManager.getDefault();
        }
        return mProfile;
    }

    private void saveAppList() {
        if (isChanged) {
            StringBuilder sb = new StringBuilder();
            int i = 0;
            int size = apps.size();
            for (String pks : apps) {
                i++;
                sb.append(pks);
                if (i < size) {
                    sb.append(',');
                }
            }
            getmProfile().setAppList(sb.toString());
        }
        isChanged = false;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        setListAdapter(adapter);
    }

    @Override
    public void onPause() {
        super.onPause();
        saveAppList();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        AppPackage pkg = adapter.getItem(position);
        pkg.selected = !pkg.selected;
        CheckBox checkbox = (CheckBox) v.findViewById(R.id.checked);
        checkbox.setChecked(pkg.selected);
        if (pkg.selected) {
            apps.add(pkg.info.packageName);
        } else {
            apps.remove(pkg.info.packageName);
        }
        isChanged = true;
    }
}
