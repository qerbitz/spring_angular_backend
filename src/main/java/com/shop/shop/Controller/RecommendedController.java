package com.shop.shop.Controller;

import com.shop.shop.Entity.Order;
import com.shop.shop.Entity.Product;
import com.shop.shop.Entity.Size_Age;
import com.shop.shop.Entity.User;
import com.shop.shop.Service.Interface.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Controller
@RequestMapping("/product")
public class RecommendedController {

    @Autowired
    OrderService orderService;

    @Autowired
    ProductService productService;

    @Autowired
    CategoryService categoryService;

    @Autowired
    UserService userService;

    @Autowired
    CartService cartService;

    @GetMapping("/recommended")
    public String test(
            @RequestParam(value = "orderId", required = false) String orderId,
            Model model) {

        int id_order=0;


        //System.out.println(orderId);
        if(orderId!=null){
                id_order= Integer.parseInt(orderId);
        }


        //Pobranie autentykacji
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = userService.getUserByUsername("seminarium2");

        Date actual_date = java.sql.Date.valueOf(LocalDate.now());


        if(user.getLast_log()==null){
            return "redirect:/product/productList/1";
        }

        //Obliczanie czasu od ostatniego zalogowania(Jezeli ponizej miesiaca nie ma proponowania dla zamowien)
        long diff_last_log = Math.abs(actual_date.getTime() - user.getLast_log().getTime());
        long diff_last_log_month = diff_last_log/ (24 * 60 * 60 * 1000) / 30;


        //System.out.println(diff_last_log_month);

        if(diff_last_log_month<1){
            return "redirect:/product/productList/1";
        }

        List<Product> recommendedNewList = new ArrayList<>();
        List<Product> recommendedNewListAllYear = new ArrayList<>();

        List<Order> list_of_user_orders = orderService.getAllOrdersByUser(user);

        //Sprawdzamy czy nie wykracza poza zamowienia
        if(id_order>=list_of_user_orders.size()){
                id_order=0;
        }
        //Jezeli cofamy
        if(id_order==-1){
            id_order=list_of_user_orders.size()-1;
        }

        Order order = list_of_user_orders.get(id_order);


        long diff = Math.abs(actual_date.getTime() - order.getOrderDate().getTime());
        long diff_months = diff / (24 * 60 * 60 * 1000) / 30;

        System.out.println("Roznica w miesiacach: " + diff_months);


        List<Product> proponowane = new ArrayList<>();
        for (int i = 0; i < order.getCart().getCartItems().size(); i++) {
            String dawny_rozmiar = order.getCart().getCartItems().get(i).getProduct().getSize_age().getProduct_age();
            String nowy_rozmiar = String.valueOf(diff_months);

            System.out.println("Dawny rozmiar:" +dawny_rozmiar);

            String[] old_size_beggining = dawny_rozmiar.split("-");
            String[] old_size_end = old_size_beggining[1].split(" msc");

            int month_add = Integer.parseInt(nowy_rozmiar);
            int new_beggining = Integer.parseInt(old_size_beggining[0]) + month_add;
            int new_end = Integer.parseInt(old_size_end[0]) + month_add;

            System.out.println("Nowy szukany przedział wiekowy: " + new_beggining + "-" + new_end + " msc");
            String season = "";


            //Styczen, Luty
            if (isBetween(1, 2, actual_date.getMonth() + 1)) {
                season = "Zimowy";
            }
            //Marzec, Kwiecien
            if (isBetween(3, 4, actual_date.getMonth() + 1)) {
                season = "Przejściowy";
            }
            //Maj, Sierpien
            if (isBetween(5, 8, actual_date.getMonth() + 1)) {
                season = "Letni";
            }
            //Wrzesien, Październik
            if (isBetween(9, 10, actual_date.getMonth() + 1)) {
                System.out.println("Zgadza sie");
                season = "Przejściowy";
            }
            //Listopad, Grudzien
            if (isBetween(11, 12, actual_date.getMonth() + 1)) {
                season = "Zimowy";
            }
            recommendedNewList = productService.getListOfProductsByAgeContaining(new_beggining, new_end, proponowane, season);
            recommendedNewListAllYear = productService.getListOfProductsByAgeContaining(new_beggining, new_end, proponowane, "Całoroczne");
        }



        //Total dla kwoty w koszyku
        model.addAttribute("total", cartService.getTotalPrice(user.getCart().getId_cart()));
        //Lista rekomendowanych produktów
        model.addAttribute("recommendedList", recommendedNewList);
        //Lista aktualnego zamowienia
        model.addAttribute("cart", list_of_user_orders.get(id_order).getCart().getCartItems());
        //Wyswietlanie nr zamowienia
        model.addAttribute("order", order);
        //Wszystkie zamowienia uzytkownika
        model.addAttribute("actual", id_order);
        return "recommendation/recommendation";

    }

    @GetMapping("/similiar")
    public String similiarProducts(Model model){


        add_to_model(model);

        return "product/index";
    }

    @PostMapping("/similiar")
    public String similiarProducts(@RequestParam(value = "category", required = false, defaultValue = "0") int category,
                                   @RequestParam(value = "size", required = false, defaultValue = "") String size,
                                   @RequestParam(value = "gender", required = false, defaultValue = "") String gender,
                                   @RequestParam(value = "season", required = false, defaultValue = "") String season,
                                   Model model) {


        int i = 0;
        int suma = 0;

        System.out.println(category);

        Size_Age size_age = new Size_Age();
        size_age.setProduct_size(size);

        Product product = new Product();
        product.setId_category(categoryService.getCategoryById(category));
        product.setSeason(season);
        product.setGender(gender);
        product.setSize_age(size_age);

        List<Product> all_products_list= productService.getListOfProducts();
        List<Product> similiar_products_list = new ArrayList<>();
        List<Double> wspolczynnik = new ArrayList<>();



        for(int j=0; j<all_products_list.size(); j++){

            //Sprawdzanie sezonu
            if(product.getSeason().equals(all_products_list.get(j).getSeason())){
                i++;
                suma++;
                System.out.println("sezon ok");
            }
            else {
                suma++;
            }
            //Sprawdzanie rozmiaru
            if(product.getSize_age().getProduct_size().equals(all_products_list.get(j).getSize_age().getProduct_size())){
                i++;
                suma++;
                System.out.println("Rozmiar ok");
            }
            else {
                suma++;
            }
            //Sprawdzanie Kategorii
            if(category!=0){
                if(product.getId_category().getName().equals(all_products_list.get(j).getId_category().getName())){
                    i++;
                    suma++;
                    System.out.println("kategoria ok");
                }
            }
            else {
                suma++;
            }
            //Sprawdzanie Plci
            if(product.getGender().equals(all_products_list.get(j).getGender())){
                i++;
                suma++;
                System.out.println("plec ok");
            }
            else {
                suma++;
            }


            double a = i;
            double b = suma;

            System.out.println(a/b);
            System.out.println("//////////////////////////////////////////////////////////");


            wspolczynnik.add(a/b);

            System.out.println(all_products_list.get(j).getName()+" "+wspolczynnik.get(j));


            if(wspolczynnik.get(j)==1.0){
                similiar_products_list.add(all_products_list.get(j));
            }


            i=0;
            suma=0;


            a=0;
            b=0;
        }


        System.out.println("'''''''''''''''''''''''''''''''''''''");



        if(category!=0){
            model.addAttribute("sizesList", productService.getListOfSizesBy(category));
        }
        add_to_model(model);
        model.addAttribute("productList",similiar_products_list);

        return "product/index";
    }

    public static boolean isBetween(int a, int b, int c) {
        return b > a ? c >= a && c <= b : c >= b && c < a;
    }

    public void add_to_model(Model model){
        model.addAttribute("categoryList", categoryService.getListOfCategories());
        //model.addAttribute("agesList", productService.getListOfAges());
        model.addAttribute("genderList", productService.getListOfGenders());
        model.addAttribute("seasonList", productService.getListOfSeasons());
        model.addAttribute("producentList",productService.getListOfProducents());
    }


}
