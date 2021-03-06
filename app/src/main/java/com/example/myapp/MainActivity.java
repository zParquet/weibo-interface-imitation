package com.example.myapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Message;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.example.myapp.objects.UserInfo;
import com.example.myapp.ui.chat.database.SQLiteDB;
import com.example.myapp.ui.discover.DiscoverFragment;
import com.example.myapp.ui.home.HomeFragment;
import com.example.myapp.ui.message.MessageFragment;
import com.example.myapp.ui.personal.PersonalFragment;
import com.example.myapp.ui.login.RegisterActivity;
import com.example.myapp.utils.Global;
import com.example.myapp.utils.Utils;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private EditText usernameEditText;
    private EditText passwordEditText;
    private Toolbar toolbar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //在登入app时，先做一些全局性初始化工作
        globalInit();

        //根据sharedPreferences判断是否已经登录
        SharedPreferences sharedPreferences = getPreferences(Context.MODE_PRIVATE);
        //已经登录：渲染主界面
        if (sharedPreferences.getBoolean("isLogin", false)) {
            //先登录
            String userEmail = sharedPreferences.getString("userEmail", "");
            String userPwd = sharedPreferences.getString("userPwd", "");
            //TODO: 向服务器发请求


            renderMainActivity();
        }
        //未登录：渲染登录界面
        else {
            renderLoginActivity();
        }

    }

    private void globalInit() {
        //创建缓存文件夹
        File cacheDir = getApplicationContext().getCacheDir();
        if (!cacheDir.exists()) {
            boolean result = cacheDir.mkdirs();
            if (!result) {
                System.out.println("创建缓存文件夹失败！");
            }
        }

        //把默认图片下载下来：只有这张图片可能产生写冲突，提前写下来
        File cacheFile = new File(cacheDir, Global.defaultImg);
        if (!cacheFile.exists()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        HttpURLConnection conn;
                        conn = (HttpURLConnection) new URL(Global.server_addr_static_profile + Global.defaultImg).openConnection();
                        conn.setRequestMethod("GET");
                        conn.connect();
                        int responseCode = conn.getResponseCode();
                        if (responseCode == HttpURLConnection.HTTP_OK) {
                            //获取服务器响应头中的流
                            InputStream is = conn.getInputStream();

                            //读取服务器返回流里的数据，把数据写入到本地，缓冲起来
                            cacheFile.createNewFile();
                            FileOutputStream fos = new FileOutputStream(cacheFile);

                            byte[] b = new byte[1024];
                            int len = 0;
                            while ((len = is.read(b)) != -1) {
                                fos.write(b, 0, len);
                            }
                            fos.close();
                            is.close();
                        } else {
                            throw new Exception("HTTP request return not 200");
                        }
                    } catch (Exception e) {
                        Utils.showToastInCenter(getApplicationContext(), e.toString(), Utils.TOAST_THREAD_QUEUE);
                    }
                }
            }).start();

        }

        //把本人的profile和id存下来，留给ChatActivity用
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String url = Global.server_addr + "/cover?sessionId=" + Global.getSessionId();
                    HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                    connection.setRequestMethod("GET");
                    connection.connect();
                    int responseCode = connection.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        InputStream inputStream = connection.getInputStream();
                        String msg = new BufferedReader(new InputStreamReader(inputStream)).readLine();
                        JSONObject jsonObject = new JSONObject(msg);
                        JSONObject jsonInfo = jsonObject.getJSONObject("info");
                        Global.myProfile = jsonInfo.getString("profile");
                        Global.myId = jsonInfo.getInt("id");
                    } else {
                        InputStream inputStream = connection.getInputStream();
                        String msg = new BufferedReader(new InputStreamReader(inputStream)).readLine();
                        System.out.println("error!" + msg);
                    }
                } catch (Exception e) {
                    Utils.showToastInCenter(getApplicationContext(), e.toString(), Utils.TOAST_THREAD_QUEUE);
                }

            }
        }).start();


//创建数据库
        Global.db = SQLiteDB.getInstance(getApplicationContext());
    }

    //加载主页面
    private void renderMainActivity() {
        //标题栏
        setContentView(R.layout.activity_main);


        //底部导航栏
        BottomNavigationView navView = findViewById(R.id.nav_view);
//        // Passing each menu ID as a set of Ids because each
//        // menu should be considered as top level destinations.
//        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
//                R.id.navigation_home, R.id.navigation_discover, R.id.navigation_message, R.id.navigation_personal)
//                .build();
//        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
//        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
//        NavigationUI.setupWithNavController(navView, navController);
        navView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                FragmentManager manager = getSupportFragmentManager();
                FragmentTransaction transaction = manager.beginTransaction();
                Fragment fragment = null;
                switch (item.getItemId()) {
                    case R.id.navigation_home:
                        fragment = new HomeFragment();
                        break;
                    case R.id.navigation_discover:
                        fragment = new DiscoverFragment();
                        break;
                    case R.id.navigation_message:
                        fragment = new MessageFragment();
                        break;
                    case R.id.navigation_personal:
                        fragment = new PersonalFragment();
                        break;
                    default:
                        break;
                }
                transaction.replace(R.id.nav_host_fragment, fragment);
                transaction.commit();
                return true;
            }
        });

    }

    //加载登录界面
    private void renderLoginActivity() {
        setContentView(R.layout.activity_login);

        usernameEditText = findViewById(R.id.login_email);
        passwordEditText = findViewById(R.id.login_pwd);
        Button loginButton = findViewById(R.id.btn_login);
        Button registerButton = findViewById(R.id.btn_register);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                login();
            }
        });
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                jumpToRegister();
            }
        });
    }

    //点击登录按钮
    private void login() {
        //TODO: for test
        renderMainActivity();

        //TODO:
//        String email = usernameEditText.getText().toString();
//        String password = passwordEditText.getText().toString();
//        if(email.equals("") || password.equals("")) {
//            Utils.showToastInCenter(getApplicationContext(), "请输入邮箱和密码！", Utils.TOAST_UI_QUEUE);
//            return ;
//        }
//        //TODO:给服务器发消息
//        //TODO:处理返回值。登录失败：提醒；登录成功：保存email和password,渲染主界面
//        new Thread(new Runnable() {
//            public void run() {
//                try {
//                    HttpURLConnection connection = (HttpURLConnection) new URL(Global.server_addr + "login").openConnection();
//                    connection.setRequestMethod("POST");
//                    connection.setDoOutput(true);
//                    connection.setDoInput(true);
//                    connection.connect();
//                    String body = "mail=" + email + "&password=" + password;
//                    OutputStream outputStream = connection.getOutputStream();
//                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
//                    writer.write(body);
//                    writer.close();
//
//                    //注意：这里的code并不是服务器手动返回的code，是http请求的code，如果不是200就肯定出错了
//                    int responseCode = connection.getResponseCode();
//                    if (responseCode == HttpURLConnection.HTTP_OK) {
//                        renderMainActivity();
//                    }
//                    else {
//                        //TODO:
//                        throw new Exception("登录失败");
//                    }
//                } catch (Exception e) {
//                    Utils.showToastInCenter(getApplicationContext(), e.toString(), Utils.TOAST_THREAD_QUEUE);
//                }
//            }
//        }).start();


    }

    //点击注册按钮
    private void jumpToRegister() {
        Intent registerIntent = new Intent(this, RegisterActivity.class);
        startActivity(registerIntent);
    }

}
