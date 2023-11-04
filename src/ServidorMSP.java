import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class ServidorMSP {
    private static final int PUERTO = 18;
    private static final List<ManejadorCliente> clientes = new ArrayList<>();

    public static void main(String[] args) {
        try {
            ServerSocket servidorSocket = new ServerSocket(PUERTO);
            System.out.println("Servidor MSP está en el puerto " + PUERTO);

            while (true) {
                Socket socketCliente = servidorSocket.accept();
                System.out.println("Nuevo cliente conectado: " + socketCliente.getInetAddress());

                ManejadorCliente cliente = new ManejadorCliente(socketCliente);
                clientes.add(cliente);
                new Thread(cliente).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class ManejadorCliente implements Runnable {
        private Socket socketCliente;
        private PrintWriter salida;
        private BufferedReader entrada;
        private String nombreUsuario;

        public ManejadorCliente(Socket socket) {
            this.socketCliente = socket;
        }

        @Override
        public void run() {
            try {
                salida = new PrintWriter(socketCliente.getOutputStream(), true);
                entrada = new BufferedReader(new InputStreamReader(socketCliente.getInputStream()));

                String lineaEntrada;
                while ((lineaEntrada = entrada.readLine()) != null) {
                    if (lineaEntrada.startsWith("CONECTAR")) {
                        manejarConexion(lineaEntrada);
                    } else if (lineaEntrada.startsWith("DESCONECTAR")) {
                        manejarDesconexion();
                    } else if (lineaEntrada.startsWith("ENVIAR")) {
                        manejarEnvio(lineaEntrada);
                    } else if (lineaEntrada.equals("LISTAR")) {
                        manejarLista();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    socketCliente.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                clientes.remove(this);
                System.out.println(nombreUsuario + " desconectado.");
            }
        }

        private void manejarConexion(String lineaEntrada) {
            String[] partes = lineaEntrada.split(" ");
            if (partes.length == 2) {
                nombreUsuario = partes[1];
                salida.println("Conectado al Servidor");
            }
        }

        private void manejarDesconexion() {
            salida.println("Desconectado del Servidor");
            try {
                socketCliente.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void manejarEnvio(String lineaEntrada) {
            if (nombreUsuario != null) {
                String mensaje = lineaEntrada.substring(lineaEntrada.indexOf("#") + 1, lineaEntrada.indexOf("@"));
                String destinatario = lineaEntrada.substring(lineaEntrada.indexOf("@") + 1);
                difundirMensaje(nombreUsuario + ": " + mensaje, destinatario);
            }
        }

        private void manejarLista() {
            salida.println("Usuarios Conectados:");
            for (ManejadorCliente cliente : clientes) {
                if (cliente.nombreUsuario != null) {
                    salida.println("- " + cliente.nombreUsuario);
                }
            }
        }

        private void difundirMensaje(String mensaje, String destinatario) {
            for (ManejadorCliente cliente : clientes) {
                if (cliente.nombreUsuario != null && cliente.nombreUsuario.equals(destinatario)) {
                    cliente.salida.println("Mensaje: " + mensaje);
                }
            }
        }
    }
}
