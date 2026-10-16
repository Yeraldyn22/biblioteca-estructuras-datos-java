package actividad6;
import java.util.*;

/**
 * ACTIVIDAD 6 - Sistema de biblioteca (libros y usuarios)
 *
 * Estructuras utilizadas:
 *  - NO LINEALES:
 *      * Arbol binario de busqueda (ABB) -> registrar y buscar libros y usuarios por codigo.
 *      * Grafo con listas de adyacencia   -> relacion "usuario tiene prestado libro".
 *  - LINEALES:
 *      * Lista enlazada (LinkedList)      -> lista de adyacencia de cada nodo del grafo.
 *      * Pila (ArrayDeque con push/pop)   -> historial (el movimiento mas reciente sale primero).
 *      * Cola (Queue)                     -> lista de espera por libro (FIFO).
 *      * Arreglo dinamico (ArrayList)     -> recorridos en orden para mostrar datos.
 */
public class Actividad6 {

    // ---------------------------------------------------------------
    // CLASES DE DATOS
    // ---------------------------------------------------------------
    static class Usuario {
        int codigo;
        String nombre;

        Usuario(int codigo, String nombre) {
            this.codigo = codigo;
            this.nombre = nombre;
        }
    }

    static class Libro {
        int codigo;
        String titulo;

        Libro(int codigo, String titulo) {
            this.codigo = codigo;
            this.titulo = titulo;
        }
    }

    // ---------------------------------------------------------------
    // ESTRUCTURA NO LINEAL 1: ARBOL BINARIO DE BUSQUEDA
    // ---------------------------------------------------------------
    static class Nodo<T> {
        int clave;
        T dato;
        Nodo<T> izquierda, derecha;

        Nodo(int clave, T dato) {
            this.clave = clave;
            this.dato = dato;
        }
    }

    static class Arbol<T> {
        Nodo<T> raiz;

        // Retorna false si el codigo ya existe
        boolean insertar(int clave, T dato) {
            Nodo<T> nuevo = new Nodo<>(clave, dato);
            if (raiz == null) {
                raiz = nuevo;
                return true;
            }
            Nodo<T> actual = raiz;
            while (true) {
                if (clave == actual.clave) {
                    return false; // repetido
                }
                if (clave < actual.clave) {          // me voy a la izquierda
                    if (actual.izquierda == null) {
                        actual.izquierda = nuevo;
                        return true;
                    }
                    actual = actual.izquierda;
                } else {                              // me voy a la derecha
                    if (actual.derecha == null) {
                        actual.derecha = nuevo;
                        return true;
                    }
                    actual = actual.derecha;
                }
            }
        }

        // Retorna el dato o null si no existe
        T buscar(int clave) {
            Nodo<T> actual = raiz;
            while (actual != null) {
                if (clave == actual.clave) {
                    return actual.dato;
                }
                actual = (clave < actual.clave) ? actual.izquierda : actual.derecha;
            }
            return null;
        }

        // Recorrido inorden: devuelve los datos ordenados por codigo
        List<T> enOrden() {
            List<T> lista = new ArrayList<>();
            recorrer(raiz, lista);
            return lista;
        }

        private void recorrer(Nodo<T> nodo, List<T> lista) {
            if (nodo == null) return;
            recorrer(nodo.izquierda, lista);
            lista.add(nodo.dato);
            recorrer(nodo.derecha, lista);
        }

        // Elimina el nodo con la clave dada (si existe)
        void eliminar(int clave) {
            raiz = eliminarNodo(raiz, clave);
        }

        private Nodo<T> eliminarNodo(Nodo<T> nodo, int clave) {
            if (nodo == null) return null;
            if (clave < nodo.clave) {
                nodo.izquierda = eliminarNodo(nodo.izquierda, clave);
            } else if (clave > nodo.clave) {
                nodo.derecha = eliminarNodo(nodo.derecha, clave);
            } else {
                // Caso 1 y 2: sin hijos o con un solo hijo
                if (nodo.izquierda == null) return nodo.derecha;
                if (nodo.derecha == null) return nodo.izquierda;
                // Caso 3: dos hijos -> se reemplaza por el sucesor inorden
                Nodo<T> sucesor = nodo.derecha;
                while (sucesor.izquierda != null) {
                    sucesor = sucesor.izquierda;
                }
                nodo.clave = sucesor.clave;
                nodo.dato = sucesor.dato;
                nodo.derecha = eliminarNodo(nodo.derecha, sucesor.clave);
            }
            return nodo;
        }

        boolean estaVacio() {
            return raiz == null;
        }
    }

    // ---------------------------------------------------------------
    // ESTRUCTURA NO LINEAL 2: GRAFO (LISTAS DE ADYACENCIA)
    // Nodos: "U<codigo>" para usuarios y "L<codigo>" para libros
    // ---------------------------------------------------------------
    static class Grafo {
        private final Map<String, LinkedList<String>> adyacencia = new HashMap<>();

        void agregarNodo(String nodo) {
            adyacencia.putIfAbsent(nodo, new LinkedList<>());
        }

        void conectar(String a, String b) {
            adyacencia.get(a).add(b);
            adyacencia.get(b).add(a);
        }

        void desconectar(String a, String b) {
            adyacencia.get(a).remove(b);
            adyacencia.get(b).remove(a);
        }

        boolean estanConectados(String a, String b) {
            LinkedList<String> vecinos = adyacencia.get(a);
            return vecinos != null && vecinos.contains(b);
        }

        LinkedList<String> vecinos(String nodo) {
            LinkedList<String> v = adyacencia.get(nodo);
            return (v == null) ? new LinkedList<>() : v;
        }

        // Elimina el nodo y todas las aristas que llegan a el
        void eliminarNodo(String nodo) {
            LinkedList<String> v = adyacencia.get(nodo);
            if (v == null) return;
            for (String vecino : v) {
                adyacencia.get(vecino).remove(nodo);
            }
            adyacencia.remove(nodo);
        }
    }

    // ---------------------------------------------------------------
    // DATOS DEL SISTEMA
    // ---------------------------------------------------------------
    static Arbol<Libro> libros = new Arbol<>();
    static Arbol<Usuario> usuarios = new Arbol<>();
    static Grafo grafo = new Grafo();

    // PILA: el ultimo movimiento queda arriba
    static Deque<String> historial = new ArrayDeque<>();

    // COLA: lista de espera por libro (codigo del libro -> usuarios en espera)
    static Map<Integer, Queue<Integer>> listaEspera = new HashMap<>();

    static Scanner sc = new Scanner(System.in);

    // ---------------------------------------------------------------
    // LECTURA SEGURA DE DATOS
    // ---------------------------------------------------------------
    static int leerEntero(String mensaje) {
        while (true) {
            System.out.print(mensaje);
            String linea = sc.nextLine().trim();
            try {
                int valor = Integer.parseInt(linea);
                if (valor < 0) {
                    System.out.println("  El valor no puede ser negativo.");
                } else {
                    return valor;
                }
            } catch (NumberFormatException e) {
                System.out.println("  Debe ingresar un numero valido (no puede quedar vacio).");
            }
        }
    }

    static String leerTexto(String mensaje) {
        while (true) {
            System.out.print(mensaje);
            String linea = sc.nextLine().trim();
            if (!linea.isEmpty()) {
                return linea;
            }
            System.out.println("  Este campo no puede quedar vacio.");
        }
    }

    // ---------------------------------------------------------------
    // MAIN + MENU
    // ---------------------------------------------------------------
    public static void main(String[] args) {
        int opcion;
        do {
            System.out.println("\n===== BIBLIOTECA =====");
            System.out.println("1. Registrar usuario");
            System.out.println("2. Registrar libro");
            System.out.println("3. Prestar libro");
            System.out.println("4. Devolver libro");
            System.out.println("5. Consultar (usuarios, libros, prestamos, historial)");
            System.out.println("6. Buscar libro o usuario por codigo");
            System.out.println("7. Eliminar libro o usuario");
            System.out.println("0. Salir");
            opcion = leerEntero("Seleccione una opcion: ");

            switch (opcion) {
                case 1:
                    registrarUsuario();
                    break;
                case 2:
                    registrarLibro();
                    break;
                case 3:
                    prestarLibro();
                    break;
                case 4:
                    devolverLibro();
                    break;
                case 5:
                    consultar();
                    break;
                case 6:
                    buscar();
                    break;
                case 7:
                    eliminar();
                    break;
                case 0:
                    System.out.println("Hasta luego.");
                    break;
                default:
                    System.out.println("Opcion no valida.");
            }
        } while (opcion != 0);
    }

    // ---------------------------------------------------------------
    // CASE 1: REGISTRAR USUARIO
    // ---------------------------------------------------------------
    static void registrarUsuario() {
        int codigo = leerEntero("Codigo del usuario: ");
        String nombre = leerTexto("Nombre del usuario: ");

        if (usuarios.insertar(codigo, new Usuario(codigo, nombre))) {
            grafo.agregarNodo("U" + codigo); // nodo usuario en el grafo
            historial.push("Se registro el usuario " + nombre + " (codigo " + codigo + ")");
            System.out.println("Usuario registrado correctamente.");
        } else {
            System.out.println("Ya existe un usuario con ese codigo.");
        }
    }

    // ---------------------------------------------------------------
    // CASE 2: REGISTRAR LIBRO
    // ---------------------------------------------------------------
    static void registrarLibro() {
        int codigo = leerEntero("Codigo del libro: ");
        String titulo = leerTexto("Titulo del libro: ");

        if (libros.insertar(codigo, new Libro(codigo, titulo))) {
            grafo.agregarNodo("L" + codigo); // nodo libro en el grafo
            historial.push("Se registro el libro '" + titulo + "' (codigo " + codigo + ")");
            System.out.println("Libro registrado correctamente.");
        } else {
            System.out.println("Ya existe un libro con ese codigo.");
        }
    }

    // ---------------------------------------------------------------
    // CASE 3: PRESTAR LIBRO
    // ---------------------------------------------------------------
    static void prestarLibro() {
        if (usuarios.estaVacio() || libros.estaVacio()) {
            System.out.println("No puedo prestar: debe haber al menos un usuario y un libro registrados.");
            return;
        }

        int codUsuario = leerEntero("Codigo del usuario: ");
        Usuario usuario = usuarios.buscar(codUsuario);
        if (usuario == null) {
            System.out.println("El usuario no existe.");
            return;
        }

        int codLibro = leerEntero("Codigo del libro: ");
        Libro libro = libros.buscar(codLibro);
        if (libro == null) {
            System.out.println("El libro no existe.");
            return;
        }

        // Si el nodo libro ya tiene un vecino, el libro esta prestado
        if (!grafo.vecinos("L" + codLibro).isEmpty()) {
            System.out.println("El libro '" + libro.titulo + "' ya se encuentra prestado.");
            String r = leerTexto("Desea ponerlo en lista de espera? (s/n): ");
            if (r.equalsIgnoreCase("s")) {
                listaEspera.computeIfAbsent(codLibro, k -> new LinkedList<>()).add(codUsuario);
                historial.push(usuario.nombre + " quedo en espera del libro '" + libro.titulo + "'");
                System.out.println("Usuario agregado a la lista de espera.");
            }
            return;
        }

        realizarPrestamo(usuario, libro);
    }

    // Crea la relacion en ambas listas de adyacencia del grafo
    static void realizarPrestamo(Usuario usuario, Libro libro) {
        grafo.conectar("U" + usuario.codigo, "L" + libro.codigo);
        historial.push("Prestamo: el libro '" + libro.titulo + "' se presto al usuario " + usuario.nombre);
        System.out.println("El libro '" + libro.titulo + "' se presto al usuario " + usuario.nombre + ".");
    }

    // ---------------------------------------------------------------
    // CASE 4: DEVOLVER LIBRO
    // ---------------------------------------------------------------
    static void devolverLibro() {
        if (usuarios.estaVacio() || libros.estaVacio()) {
            System.out.println("No hay datos registrados.");
            return;
        }

        int codUsuario = leerEntero("Codigo del usuario: ");
        Usuario usuario = usuarios.buscar(codUsuario);
        if (usuario == null) {
            System.out.println("El usuario no existe.");
            return;
        }

        int codLibro = leerEntero("Codigo del libro: ");
        Libro libro = libros.buscar(codLibro);
        if (libro == null) {
            System.out.println("El libro no existe.");
            return;
        }

        if (!grafo.estanConectados("U" + codUsuario, "L" + codLibro)) {
            System.out.println("Ese usuario no tiene prestado ese libro.");
            return;
        }

        grafo.desconectar("U" + codUsuario, "L" + codLibro);
        historial.push("Devolucion: el usuario " + usuario.nombre + " devolvio '" + libro.titulo + "'");
        System.out.println("Libro devuelto correctamente.");

        // Si hay alguien esperando (cola FIFO), se le presta de una vez
        Queue<Integer> cola = listaEspera.get(codLibro);
        if (cola != null && !cola.isEmpty()) {
            int siguiente = cola.poll();
            Usuario u = usuarios.buscar(siguiente);
            if (u != null) {
                System.out.println("Habia una lista de espera: se presta al siguiente usuario.");
                realizarPrestamo(u, libro);
            }
        }
    }

    // ---------------------------------------------------------------
    // CASE 5: CONSULTAR
    // ---------------------------------------------------------------
    static void consultar() {
        System.out.println("\n--- USUARIOS REGISTRADOS ---");
        List<Usuario> listaUsuarios = usuarios.enOrden();
        if (listaUsuarios.isEmpty()) {
            System.out.println("No hay usuarios registrados.");
        }
        for (Usuario u : listaUsuarios) {
            System.out.println(u.codigo + " - " + u.nombre);
        }

        System.out.println("\n--- LIBROS REGISTRADOS ---");
        List<Libro> listaLibros = libros.enOrden();
        if (listaLibros.isEmpty()) {
            System.out.println("No hay libros registrados.");
        }
        for (Libro l : listaLibros) {
            String estado = grafo.vecinos("L" + l.codigo).isEmpty() ? "Disponible" : "Prestado";
            System.out.println(l.codigo + " - " + l.titulo + " [" + estado + "]");
        }

        System.out.println("\n--- PRESTAMOS ACTIVOS ---");
        boolean hayPrestamos = false;
        for (Usuario u : listaUsuarios) {
            for (String nodoLibro : grafo.vecinos("U" + u.codigo)) {
                int codLibro = Integer.parseInt(nodoLibro.substring(1));
                Libro l = libros.buscar(codLibro);
                System.out.println("El libro '" + l.titulo + "' esta prestado al usuario " + u.nombre);
                hayPrestamos = true;
            }
        }
        if (!hayPrestamos) {
            System.out.println("No hay prestamos activos.");
        }

        System.out.println("\n--- HISTORIAL (del mas reciente al mas antiguo) ---");
        if (historial.isEmpty()) {
            System.out.println("No hay movimientos registrados.");
        }
        for (String movimiento : historial) {
            System.out.println("* " + movimiento);
        }
    }

    // ---------------------------------------------------------------
    // CASE 6: BUSCAR POR CODIGO (usa la busqueda del arbol y el grafo)
    // ---------------------------------------------------------------
    static void buscar() {
        System.out.println("1. Buscar libro");
        System.out.println("2. Buscar usuario");
        int tipo = leerEntero("Seleccione: ");

        if (tipo == 1) {
            int cod = leerEntero("Codigo del libro: ");
            Libro l = libros.buscar(cod);
            if (l == null) {
                System.out.println("El libro no existe.");
                return;
            }
            System.out.println("Libro encontrado: " + l.titulo);
            LinkedList<String> v = grafo.vecinos("L" + cod);
            if (v.isEmpty()) {
                System.out.println("Estado: disponible.");
            } else {
                Usuario u = usuarios.buscar(Integer.parseInt(v.getFirst().substring(1)));
                System.out.println("Estado: prestado a " + u.nombre);
            }
        } else if (tipo == 2) {
            int cod = leerEntero("Codigo del usuario: ");
            Usuario u = usuarios.buscar(cod);
            if (u == null) {
                System.out.println("El usuario no existe.");
                return;
            }
            System.out.println("Usuario encontrado: " + u.nombre);
            LinkedList<String> v = grafo.vecinos("U" + cod);
            if (v.isEmpty()) {
                System.out.println("No tiene libros prestados.");
            } else {
                for (String nodo : v) {
                    Libro l = libros.buscar(Integer.parseInt(nodo.substring(1)));
                    System.out.println("Tiene prestado: " + l.titulo);
                }
            }
        } else {
            System.out.println("Opcion no valida.");
        }
    }

    // ---------------------------------------------------------------
    // CASE 7: ELIMINAR LIBRO O USUARIO (nodo del arbol y nodo del grafo)
    // ---------------------------------------------------------------
    static void eliminar() {
        System.out.println("1. Eliminar libro");
        System.out.println("2. Eliminar usuario");
        int tipo = leerEntero("Seleccione: ");

        if (tipo == 1) {
            int cod = leerEntero("Codigo del libro: ");
            Libro l = libros.buscar(cod);
            if (l == null) {
                System.out.println("El libro no existe.");
            } else if (!grafo.vecinos("L" + cod).isEmpty()) {
                System.out.println("No se puede eliminar: el libro esta prestado.");
            } else {
                libros.eliminar(cod);
                grafo.eliminarNodo("L" + cod);
                listaEspera.remove(cod);
                historial.push("Se elimino el libro '" + l.titulo + "' (codigo " + cod + ")");
                System.out.println("Libro eliminado.");
            }
        } else if (tipo == 2) {
            int cod = leerEntero("Codigo del usuario: ");
            Usuario u = usuarios.buscar(cod);
            if (u == null) {
                System.out.println("El usuario no existe.");
            } else if (!grafo.vecinos("U" + cod).isEmpty()) {
                System.out.println("No se puede eliminar: el usuario tiene libros prestados.");
            } else {
                usuarios.eliminar(cod);
                grafo.eliminarNodo("U" + cod);
                for (Queue<Integer> cola : listaEspera.values()) {
                    cola.remove(Integer.valueOf(cod));
                }
                historial.push("Se elimino el usuario " + u.nombre + " (codigo " + cod + ")");
                System.out.println("Usuario eliminado.");
            }
        } else {
            System.out.println("Opcion no valida.");
        }
    }
}