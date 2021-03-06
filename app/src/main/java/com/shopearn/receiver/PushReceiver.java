package com.shopearn.receiver;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.shopearn.R;
import com.shopearn.activity.MainActivity;
import com.shopearn.global.AppController;
import com.shopearn.global.ContainerHolderSingleton;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.tagmanager.Container;
import com.google.android.gms.tagmanager.ContainerHolder;
import com.google.android.gms.tagmanager.TagManager;

import java.util.concurrent.TimeUnit;

/**
 * Created by apple on 16/01/17.
 */

public class PushReceiver extends BroadcastReceiver {

    private static int i  ;
    private static final String CONTAINER_ID = "GTM-NB8JXWG";
    //  private static final String CONTAINER_ID = "GTM-K8XVZX";
    private String title = "";
    private String content = "";
    private String pushText = "";

    @Override
    public void onReceive(Context context, Intent intent) {
            if(AppController.getInstance().isInternetOn())
                refreshTagManager(context);
    }

    private void refreshTagManager(final Context context){


        TagManager tagManager = TagManager.getInstance(context);
        tagManager.setVerboseLoggingEnabled(true);

        com.google.android.gms.common.api.PendingResult<ContainerHolder> pending = tagManager.loadContainerPreferNonDefault(
                CONTAINER_ID,
                R.raw.gtm_analytics
        );

        pending.setResultCallback(new ResultCallback<ContainerHolder>() {
            @Override
            public void onResult(ContainerHolder containerHolder) {
                ContainerHolderSingleton.setContainerHolder(containerHolder);
                final Container container = containerHolder.getContainer();

                containerHolder.refresh();
                title = container.getString("title");
                content = container.getString("url");
                pushText = container.getString("text");

                Log.d("title" ,"titl" + title);
                sendNotification(context, title, content);

                if (!containerHolder.getStatus().isSuccess()) {
                    Log.e("IL", "failure loading container");
                    return;
                }

                ContainerHolderSingleton.setContainerHolder(containerHolder);
                ContainerLoadedCallback.registerCallbacksForContainer(container);
                containerHolder.setContainerAvailableListener(new ContainerLoadedCallback());
            }
        }, 2, TimeUnit.SECONDS);

    }

    private void sendNotification(Context context, String title , String content){
        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        SharedPreferences sp = context.getSharedPreferences("user", 0);

        String email = sp.getString("email", "guest");

//        String extraParams = "&affExtParam1=" + AppController.getInstance().getAndroidId()
//                + "&affExtParam2=" + email;



        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.shop)
                .setContentTitle(title)
                .setSound(soundUri)
                .setAutoCancel(true)
                .setContentText(pushText);



        Intent resultIntent = new Intent();

        if(content.contains("flipkart")){

            String extraParams = "&affExtParam1=" + AppController.getInstance().getAndroidId()
                    + "&affExtParam2=" + email;
            PackageManager manager = context.getPackageManager();
            try {
                Intent i = manager.getLaunchIntentForPackage("com.flipkart.android");
                if (i == null) {
                    throw new PackageManager.NameNotFoundException();
                }
                i.addCategory(Intent.ACTION_VIEW);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                i.setData(Uri.parse(content + extraParams));
                resultIntent = i;

            } catch (PackageManager.NameNotFoundException e) {
                Intent startIntent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("http://affiliate.flipkart.com/install-app?affid=ashuinbit"+ extraParams));

                startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(startIntent);
            }
        }
        else{
            if(content.contains("amazon")) {
                try {
                    Intent startIntent = new Intent(Intent.ACTION_VIEW,
                            Uri.parse(content + AppController.getInstance().getAndroidId() + "~~email~~" + email));

                    startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                    resultIntent.setData(Uri.parse(content + AppController.getInstance().getAndroidId() + "~~email~~" + email));
//                    context.startActivity(new Intent(Intent.ACTION_VIEW,
//                            Uri.parse(content + AppController.getInstance().getAndroidId() + "~~email~~" + email)));

                    resultIntent = startIntent;

                } catch (ActivityNotFoundException e) {
                                context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=in.amazon.mShop.android.shopping")));
                }
            }

            else if(content.contains("snapdeal")){
                Intent startIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(content+ "&aff_sub=" +
                        email+ "&aff_sub2=abc" + AppController.getInstance().getAndroidId()));

                startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                resultIntent = startIntent;

//                getActivity().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url+ "&aff_sub=" +
//                        email+ "&aff_sub2=abc" + AppController.getInstance().getAndroidId())));
            }
        }

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);

        stackBuilder.addParentStack(MainActivity.class);

        stackBuilder.addNextIntent(resultIntent);

        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        builder.setContentIntent(resultPendingIntent);

        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
// mId allows you to update the notification later on.
        mNotificationManager.notify(i, builder.build());
    }

    private static class ContainerLoadedCallback implements ContainerHolder.ContainerAvailableListener {
        @Override
        public void onContainerAvailable(ContainerHolder containerHolder, String containerVersion) {
            // We load each container when it becomes available.
            Container container = containerHolder.getContainer();
            registerCallbacksForContainer(container);
        }

        public static void registerCallbacksForContainer(Container container) {
            // Register two custom function call macros to the container.
        }
    }

}
