package com.example.hackinhome2021_stankinfood.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.hackinhome2021_stankinfood.R;
import com.example.hackinhome2021_stankinfood.fragments.AuthRegChooseFragment;
import com.example.hackinhome2021_stankinfood.fragments.AuthRegFragment;
import com.example.hackinhome2021_stankinfood.fragments.CartFragment;
import com.example.hackinhome2021_stankinfood.fragments.MenuFragment;
import com.example.hackinhome2021_stankinfood.fragments.OrderFragment;
import com.example.hackinhome2021_stankinfood.fragments.OrdersFragment;
import com.example.hackinhome2021_stankinfood.fragments.ProductFragment;
import com.example.hackinhome2021_stankinfood.fragments.RestaurantsFragment;
import com.example.hackinhome2021_stankinfood.interfaces.OnBackPressedFragment;
import com.example.hackinhome2021_stankinfood.models.Order;
import com.example.hackinhome2021_stankinfood.models.Product;
import com.example.hackinhome2021_stankinfood.models.Restaurant;
import com.example.hackinhome2021_stankinfood.models.User;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.StorageReference;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import org.apache.commons.net.time.TimeTCPClient;

import java.io.IOException;
import java.security.SecureRandom;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends AppCompatActivity
        implements BottomNavigationView.OnNavigationItemSelectedListener {
    private static final String TAG = "LOG_MESSAGE";

    private static final String AUTH_REG_CHOOSE_FRAGMENT = "AUTH_REG_CHOOSE_FRAGMENT";
    private static final String AUTH_REG_FRAGMENT = "AUTH_REG_FRAGMENT";
    private static final String MENU_FRAGMENT = "MENU_FRAGMENT";
    private static final String PRODUCT_FRAGMENT = "PRODUCT_FRAGMENT";

    public static final int MENU_HEADER = 0;
    public static final int MENU_PRODUCT_INACTIVE = 1;
    public static final int MENU_PRODUCT_ACTIVE = 2;
    public static final int ORDER_PRODUCT_INACTIVE = 3;
    public static final int ORDER_PRODUCT_ACTIVE = 4;

    private static final String COLLECTION_RESTAURANTS = "restaurants";
    private static final String COLLECTION_ORDERS = "orders";
    private static final String COLLECTION_PRODUCTS = "products";
    private static final String COLLECTION_FAVORITE_ORDERS = "favoriteOrders";
    private static final String COLLECTION_USERS = "users";


    private final Random random = new SecureRandom();
    private final List<String> categoriesNames = Arrays.asList(
            "Супы", "Мясо", "Напитки");
    private final List<String> soupNames = Arrays.asList(
            "Харчо", "Затируха", "Томатный",
            "Куриный", "Любимый", "Солянка", "Гречневый");
    private final List<String> meatNames = Arrays.asList(
            "Свиннина", "Говядина", "Телятина",
            "Баранина", "Крольчатина", "Оленина", "Ягнятина");
    private final List<String> drinkNames = Arrays.asList(
            "7UP", "Lipton", "AQUA",
            "Mirinda", "MountainDew", "Pepsi", "Drive");


    private String currentWeekday;
    private Date currentDate = null;
    public static Order userOrder = new Order();

    private View parentLayout;
    private int previousDirection = 0;
    private int previousBottomNavigationTabId;

    private BottomNavigationView bottomNavigationView;

    private CurrentTimeGetterThread currentTimeGetterThread = null;

    private User userData;
    private FirebaseUser currentUser = null;
    private final FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
    private static final int RC_SIGN_IN = 9001;
    private GoogleSignInClient googleSignInClient;

    private final FirebaseFirestore firebaseFirestore = FirebaseFirestore.getInstance();
    private StorageReference storageReference;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        parentLayout = findViewById(android.R.id.content);
        initBottomNavigationView();
        previousBottomNavigationTabId = R.id.menuItemRestaurants;

        currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            hideBottomNavigationView(true);
            replaceFragmentToAuthRegChooseFragment();
        } else {
            currentTimeGetterThread = new CurrentTimeGetterThread();
            currentTimeGetterThread.start();
        }

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);
    }


    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    private void initBottomNavigationView() {
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setOnNavigationItemSelectedListener(this);
    }

    private class CurrentTimeGetterThread extends Thread {
        @Override
        public void run() {
            TimeTCPClient client = new TimeTCPClient();

            while (true) {
                try {
                    client.connect("time.nist.gov");
                    client.setKeepAlive(false);

                    currentDate = client.getDate();
                    client.disconnect();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                    try {
                        Thread.sleep(250);
                    } catch (InterruptedException interruptedException) {
                        interruptedException.printStackTrace();
                    }
                }

                if (currentDate != null) {
                    DateFormat weekdayString = new SimpleDateFormat("EEEE", Locale.ENGLISH);
                    currentWeekday = weekdayString.format(currentDate);
                    findUserInDatabase();
                    break;
                }
            }
        }
    }


    private void generateData() {
        List<Product> productList = new ArrayList<>();

        for (String soupName : soupNames) {
            Product product = new Product();
            product.setRestaurantId("XUqz9HedsagJwuZV1ur7");
            product.setProductsLeft(getRandomInteger(0, 10));
            product.setProductName(soupName);
            product.setPrice(getRandomInteger(50, 100));
            product.setLikesCount(getRandomInteger(0, 20));
            product.setImageURL(getRandomString(100));
            product.setDescription(getRandomString(255));
            product.setCategoryName(categoriesNames.get(0));
            productList.add(product);
        }

        for (String meatName : meatNames) {
            Product product = new Product();
            product.setRestaurantId("XUqz9HedsagJwuZV1ur7");
            product.setProductsLeft(getRandomInteger(0, 10));
            product.setProductName(meatName);
            product.setPrice(getRandomInteger(50, 100));
            product.setLikesCount(getRandomInteger(0, 20));
            product.setImageURL(getRandomString(100));
            product.setDescription(getRandomString(255));
            product.setCategoryName(categoriesNames.get(1));
            productList.add(product);
        }

        for (String drinkName : drinkNames) {
            Product product = new Product();
            product.setRestaurantId("XUqz9HedsagJwuZV1ur7");
            product.setProductsLeft(getRandomInteger(0, 10));
            product.setProductName(drinkName);
            product.setPrice(getRandomInteger(50, 100));
            product.setLikesCount(getRandomInteger(0, 20));
            product.setImageURL(getRandomString(100));
            product.setDescription(getRandomString(255));
            product.setCategoryName(categoriesNames.get(2));
            productList.add(product);
        }

        for (Product product : productList) {
            firebaseFirestore.collection(COLLECTION_PRODUCTS).add(product);
        }
    }

    private String getRandomString(int length) {
        int leftLimit = 97;     // letter 'a'
        int rightLimit = 122;   // letter 'z'

        StringBuilder buffer = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int randomLimitedInt = leftLimit +
                    (int) (random.nextFloat() * (rightLimit - leftLimit + 1));
            buffer.append((char) randomLimitedInt);
        }

        return buffer.toString();
    }

    private int getRandomInteger(int min, int max) {
        return random.nextInt(max - min) + min;
    }


    public void createUserWithEmailAndPassword(String email, String password) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "createUserWithEmailAndPassword(): Task Successful!");
                        hideKeyboard(this);

                        currentUser = firebaseAuth.getCurrentUser();
                        currentUser.sendEmailVerification();

                        Fragment fragment = getSupportFragmentManager().findFragmentByTag(AUTH_REG_FRAGMENT);
                        ((AuthRegFragment) fragment).showAlertDialogVerificationMessage(email);
                    } else {
                        Log.d(TAG, "createUserWithEmailAndPassword(): Task Failure!");
                    }
                });
    }

    public void authUserWithEmailAndPassword(String email, String password) {
        hideKeyboard(this);

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "authUserWithEmailAndPassword(): Task Successful!");
                        currentUser = firebaseAuth.getCurrentUser();
                        if (!currentUser.isEmailVerified()) {
                            Fragment fragment = getSupportFragmentManager().findFragmentByTag(AUTH_REG_FRAGMENT);
                            ((AuthRegFragment) fragment).showSnackBarEmailNotVerified();
                        } else findUserInDatabase();
                    } else {
                        Log.d(TAG, "authUserWithEmailAndPassword(): Task Failure!");
                    }
                });
    }


    public void signInWithGoogle() {
        Intent signInWithGoogle = googleSignInClient.getSignInIntent();
        startActivityForResult(signInWithGoogle, RC_SIGN_IN);
    }

    public void createIntentIntegrator() {
//        IntentIntegrator intentIntegrator = new IntentIntegrator(MainActivity.this);
//        intentIntegrator.setBeepEnabled(true);
//        intentIntegrator.setOrientationLocked(true);
//        intentIntegrator.setCaptureActivity(Capture.class);
//        intentIntegrator.initiateScan();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Log.d(TAG, "onActivityResult: " + account.getId());
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException apiException) {
                apiException.printStackTrace();
            }
        } else {
            IntentResult intentResult = IntentIntegrator.parseActivityResult(
                    requestCode, requestCode, data);

            if (intentResult != null) {
                if (intentResult.getContents() != null) {
                    Log.d(TAG, intentResult.getContents());
                    getOrderFromFireStore(intentResult.getContents());
                } else {
                    Snackbar.make(parentLayout, getResources().getString(R.string.scan_error),
                            BaseTransientBottomBar.LENGTH_SHORT).show();
                }
            }
        }
    }

    public void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {

                            Log.d(TAG, "signInWithCredential:success");
                            currentUser = firebaseAuth.getCurrentUser();
                            findUserInDatabase();
                        } else {

                            Log.w(TAG, "signInWithCredential:failure", task.getException());
//                            updateUI(null);
                        }
                    }
                });
    }


    private void findUserInDatabase() {
        User user = new User();
        user.setUserId(currentUser.getUid());
        user.setRestaurantId(null);

        firebaseFirestore.collection(COLLECTION_USERS).whereEqualTo(
                "userId", user.getUserId()).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()) {
                            userData = queryDocumentSnapshot.toObject(User.class);
                            Log.d("LOG_MESSAGE", userData.toString());
                        }
                        if (currentTimeGetterThread == null) {
                            currentTimeGetterThread = new CurrentTimeGetterThread();
                            currentTimeGetterThread.start();
                        }
                        if (task.getResult().isEmpty()) {
                            createUserInDatabase(user);
                        }
                        setBottomNavigationViewToZeroPosition();
                        getRestaurantsFromFireStore();
                        userOrder.setUserId(currentUser.getUid());
                        Log.d(TAG, "findUserInDatabase(): Task Successful!");
                    } else {
                        Log.d(TAG, "findUserInDatabase(): Task Failure!");
                    }
                });
    }

    private void createUserInDatabase(User user) {
        firebaseFirestore.collection(COLLECTION_USERS).document().set(user)
                .addOnCompleteListener(taskInner -> {
                    if (taskInner.isSuccessful()) {
                        Log.d(TAG, "createUserInDatabase(): Task Successful!");
                    } else {
                        Log.d(TAG, "createUserInDatabase(): Task Failure!");
                    }
                });
    }

    public void sendResetPasswordByEmail(String email) {
        firebaseAuth.sendPasswordResetEmail(email)
                .addOnSuccessListener(aVoid -> {
                    Fragment fragment = getSupportFragmentManager().findFragmentByTag(AUTH_REG_FRAGMENT);
                    ((AuthRegFragment) fragment).showSnackBarResetPassword(email);
                })
                .addOnFailureListener(e -> Log.d("LOG_MESSAGE", "sendResetPasswordByEmail(): Failture!"));
    }


    public void replaceFragmentToAuthRegChooseFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.mainContainer, new AuthRegChooseFragment(), AUTH_REG_CHOOSE_FRAGMENT);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    public void replaceFragmentToAuthRegFragment(boolean isRegistration) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.mainContainer, AuthRegFragment.newInstance(isRegistration), AUTH_REG_FRAGMENT);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    private void replaceRestaurantsToFragment(List<Restaurant> restaurantList) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.popBackStack();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.mainContainer, RestaurantsFragment.newInstance(restaurantList));
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    public void replaceFragmentToProductFragment(List<Product> productList, int position) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.mainContainer, ProductFragment.newInstance(
                productList.get(position)));
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    private void replaceFragmentToMenuFragment(boolean isMenu, List<Product> productList) {
        List<Product> result = getConvertedProductListForRecyclerView(productList, isMenu);

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.mainContainer, MenuFragment.newInstance(
                isMenu, result));
//        if (isMenu)
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    private void replaceFragmentToCardFragment() {
        userOrder.setViewTypeForPositions(true);

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.mainContainer, CartFragment.newInstance(userOrder));
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    public void replaceFragmentToOrderFragment(Order order) {
        boolean isManagerView = userData.getRestaurantId() != null;
        order.setViewTypeForPositions(isManagerView);

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.mainContainer, OrderFragment.newInstance(
                isManagerView, order));
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    private void replaceFragmentToOrdersFragment(List<Order> orderList) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.mainContainer, OrdersFragment.newInstance(
                userData.getRestaurantId() != null, orderList));
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    private void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        View view = activity.getCurrentFocus();

        if (view == null) {
            view = new View(activity);
        }

        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public void hideBottomNavigationView(boolean hide) {
        if (hide) {
            bottomNavigationView.setVisibility(View.GONE);
        } else bottomNavigationView.setVisibility(View.VISIBLE);
    }


    private void setBottomNavigationViewToZeroPosition() {
        previousDirection = 0;
        previousBottomNavigationTabId = R.id.menuItemRestaurants;
        bottomNavigationView.setOnNavigationItemSelectedListener(null);
        bottomNavigationView.setSelectedItemId(R.id.menuItemRestaurants);
        bottomNavigationView.setOnNavigationItemSelectedListener(this);
    }


    public void getRestaurantsFromFireStore() {
        firebaseFirestore.collection(COLLECTION_RESTAURANTS).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Restaurant> restaurantList = new ArrayList<>();
                        for (QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()) {
                            Restaurant restaurant = queryDocumentSnapshot.toObject(Restaurant.class);
                            restaurant.setRestaurantId(queryDocumentSnapshot.getId());
                            restaurantList.add(restaurant);
                        }
                        setBottomNavigationViewToZeroPosition();
                        replaceRestaurantsToFragment(restaurantList);
                    } else {
                        Log.d(TAG, "getRestraintsFromFireStore(): Failed!");
                    }
                });
    }

    private List<Product> getConvertedProductListForRecyclerView(List<Product> productList, boolean isMenu) {
        for (Product product : productList) {
            product.setRating(((float) product.getLikesCount()) / ((float) productList.size()));
            product.setViewType(MainActivity.MENU_PRODUCT_INACTIVE);
        }

        Collections.sort(productList, Product.PRODUCT_COMPARATOR);
        if (isMenu) convertForRecyclerView(productList);

        return productList;
    }

    private void convertForRecyclerView(List<Product> productList) {
        List<String> categoryNamesList = new ArrayList<>();

        for (Product product : productList) {
            String savedCategoryName = product.getCategoryName();
            if (!categoryNamesList.contains(savedCategoryName)) {
                categoryNamesList.add(savedCategoryName);
            }
        }

        int index = 0;
        String savedCategoryName = categoryNamesList.get(0);
        Product firstHeader = new Product();
        firstHeader.setCategoryName(savedCategoryName);
        firstHeader.setViewType(MainActivity.MENU_HEADER);
        productList.add(0, firstHeader);
        index++;

        for (int i = 1; i < productList.size(); i++) {
            if (!productList.get(i).getCategoryName().equals(savedCategoryName)) {
                savedCategoryName = categoryNamesList.get(index);
                Product categoryName = new Product();
                categoryName.setCategoryName(savedCategoryName);
                categoryName.setViewType(MainActivity.MENU_HEADER);
                productList.add(i, categoryName);
                index++;
            }
        }
    }

    public void getFavoriteProductsFromFireStore() {
        firebaseFirestore.collection(COLLECTION_PRODUCTS)
                .whereArrayContains("likedUserIds", currentUser.getUid())
                .whereGreaterThanOrEqualTo("productsLeft", 1).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Product> productList = new ArrayList<>();
                        for (QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()) {
                            Product product = queryDocumentSnapshot.toObject(Product.class);
                            product.setProductId(queryDocumentSnapshot.getId());
                            product.setLiked(true);
                            productList.add(product);
                        }
                        replaceFragmentToMenuFragment(false, productList);
                        Log.d(TAG, "getFavoriteProductsFromFireStore(): Success!");
                    } else {
                        Log.d(TAG, "getFavoriteProductsFromFireStore(): Failed!");
                    }
                });
    }

    public void getMenuFromFireStore(Restaurant restaurant) {
        userOrder.clearPositions();

        firebaseFirestore.collection(COLLECTION_PRODUCTS)
                .whereEqualTo("restaurantId", restaurant.getRestaurantId())
                .whereGreaterThanOrEqualTo("productsLeft", 1).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Product> productList = new ArrayList<>();
                        for (QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()) {
                            Product product = queryDocumentSnapshot.toObject(Product.class);
                            product.setProductId(queryDocumentSnapshot.getId());
                            if (product.getLikedUserIds() != null && product.getLikedUserIds().size() != 0) {
                                if (product.getLikedUserIds().contains(currentUser.getUid())) {
                                    product.setLikedUserIds(Collections.singletonList(currentUser.getUid()));
                                    product.setLiked(true);
                                } else {
                                    product.setLikedUserIds(null);
                                    product.setLiked(false);
                                }
                            }
                            productList.add(product);
                        }
                        replaceFragmentToMenuFragment(true, productList);
                        Log.d(TAG, "getProductsFromFireStore(): Success!");
                    } else {
                        Log.d(TAG, "getProductsFromFireStore(): Failed!");
                    }
                });
    }

    public void addOrderToFireStore(Order order) {
        for (Product product : order.getPositions()) {
            product.setProductsLeft(product.getCountForOrder());
        }

        firebaseFirestore.collection(COLLECTION_ORDERS).add(order)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        order.clearPositions();
                        setBottomNavigationViewToZeroPosition();
                        getRestaurantsFromFireStore();
                        Log.d(TAG, "addOrderToFireStore(): Successful!");
                    } else Log.d(TAG, "addOrderToFireStore(): Failure!");
                });
    }

    public void getRestaurantOrdersFromFireStore() {
        firebaseFirestore.collection(COLLECTION_ORDERS)
                .whereEqualTo("restaurantId", userData.getRestaurantId()).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Order> restaurantOrders = new ArrayList<>();
                        for (QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()) {
                            Order order = queryDocumentSnapshot.toObject(Order.class);
                            order.setOrderId(queryDocumentSnapshot.getId());
                            restaurantOrders.add(order);
                        }
                        replaceFragmentToOrdersFragment(restaurantOrders);
                        Log.d(TAG, "getUserOrdersFromFireStore(): Successful!");
                    } else {
                        Log.d(TAG, "getUserOrdersFromFireStore(): Failure!");
                    }
                });
    }

    public void getUserOrdersFromFireStore() {
        firebaseFirestore.collection(COLLECTION_ORDERS)
                .whereEqualTo("userId", currentUser.getUid()).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Order> userOrders = new ArrayList<>();
                        for (QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()) {
                            Order order = queryDocumentSnapshot.toObject(Order.class);
                            order.setOrderId(queryDocumentSnapshot.getId());
                            userOrders.add(order);
                        }
                        replaceFragmentToOrdersFragment(userOrders);
                        Log.d(TAG, "getUserOrdersFromFireStore(): Successful!");
                    } else {
                        Log.d(TAG, "getUserOrdersFromFireStore(): Failure!");
                    }
                });
    }

    public void getOrderFromFireStore(String orderId) {
        firebaseFirestore.collection(COLLECTION_ORDERS).document(orderId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Order order = task.getResult().toObject(Order.class);
                        replaceFragmentToOrderFragment(order);
                        Log.d(TAG, "getOrderFromFireStore(): Successful!");
                    } else {
                        Log.d(TAG, "getOrderFromFireStore(): Failed!");
                    }
                });
    }

    public void markProductAsLiked(Product product, boolean isLiked) {
        firebaseFirestore.collection(COLLECTION_PRODUCTS).document(product.getProductId())
                .update("likedUserIds", isLiked ?
                        FieldValue.arrayUnion(currentUser.getUid()) :
                        FieldValue.arrayRemove(currentUser.getUid()))
                .addOnCompleteListener(task -> {
                    String message = isLiked ? getResources().getString(R.string.like_add) :
                            getResources().getString(R.string.like_remove);
                    Snackbar.make(parentLayout, message,
                            BaseTransientBottomBar.LENGTH_SHORT).show();
                });
    }


    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id != previousBottomNavigationTabId) {
            int currentDirection = 0;
            if (id == R.id.menuItemRestaurants) {
                getRestaurantsFromFireStore();
//            } else if (id == R.id.menuItemOrders) {
//                currentDirection = 1;
//
            } else if (id == R.id.menuItemFavoriteProducts) {
                getFavoriteProductsFromFireStore();
            } else if (id == R.id.menuItemOrders) {
                if (userData.getRestaurantId() == null) {
                    getUserOrdersFromFireStore();
                } else getRestaurantOrdersFromFireStore();
            } else if (id == R.id.menuItemCart) {
                replaceFragmentToCardFragment();
            }
//            else if (id == R.id.menuItemCart) {
//                currentDirection = 3;
//                fragment = new CartFragment();
//            }
//
//            if (previousDirection < currentDirection) {
//                fragmentTransaction.setCustomAnimations(
//                        R.anim.enter_from_right, R.anim.exit_to_left,
//                        R.anim.enter_from_right, R.anim.exit_to_left);
//            } else {
//                fragmentTransaction.setCustomAnimations(
//                        R.anim.enter_from_left, R.anim.exit_to_right,
//                        R.anim.enter_from_left, R.anim.exit_to_right);
//            }
//
//            if (id == R.id.menuItemRestaurants || id == R.id.menuItemCafe) {
//                fragmentTransaction.replace(R.id.mainContainer, fragment, MENU_FRAGMENT);
//            } else fragmentTransaction.replace(R.id.mainContainer, fragment);
//            fragmentTransaction.commit();
//
            previousDirection = currentDirection;
            previousBottomNavigationTabId = id;
        }

        return true;
    }


    @Override
    public void onBackPressed() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.mainContainer);
        if (!(fragment instanceof OnBackPressedFragment) ||
                !((OnBackPressedFragment) fragment).onBackPressed()) {
            if (getSupportFragmentManager().getBackStackEntryCount() == 1) {
                finish();
            } else super.onBackPressed();
        }
    }
}