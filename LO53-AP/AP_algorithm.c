#include <stdio.h>      /* standard C i/o facilities */
#include <stdlib.h>     /* needed for atoi() */
#include <unistd.h>  	/* defines STDIN_FILENO, system calls,etc */
#include <string.h>

#include <pthread.h>
#include <pcap.h>
#include "prism.h"

#include <time.h>
#include <errno.h>

#include <netinet/in.h> /* INET constants and stuff */
#include <arpa/inet.h>  /* IP address conversion stuff */
#include <netdb.h>	/* gethostbyname */
#include <netinet/if_ether.h>

#include <sys/types.h>  /* system data type definitions */
#include <sys/socket.h> /* socket specific definitions */
#include <malloc.h>

#define MAXBUF 5000


///////////////////////////////////////////////
///											///
///					Headers					///
///											///
///////////////////////////////////////////////

struct ieee80211_header {  //header for Rssi frame
  u_short frame_control;
  u_short frame_duration;
  u_char recipient[6];
  u_char source_addr[6];
  u_char address3[6];
  u_short sequence_control;
  u_char address4[6];
};


void print_Mac_addr(char mca[12]); //just so it can be launch from everywhere

///////////////////////////////////////////////
///											///
///					Consts					///
///											///
///////////////////////////////////////////////
const u_char * packet;

///////////////////////////////////////////////
///											///
///					Structs					///
///											///
///////////////////////////////////////////////
struct prism_header *ph;
struct ieee80211_header *eh;
struct pcap_pkthdr header;

typedef struct Rssi_List { //Structure for our Rssi list
	int exp_date;
	int rssi_value;
	struct Rssi_List * next;
} Rssi_List;

typedef struct Device_List { //strucure for ou Device list (=Mac adress + rssi list)
	char mac_addr[12];
	Rssi_List * rssi_list;
	struct Device_List * next;
} Device_List;


///////////////////////////////////////////////
///											///
///					Functions				///
///											///
///////////////////////////////////////////////


///////////////////////////////////////
///			Adding a device			///
///////////////////////////////////////
void add_device(Device_List **dl, char mac_addr[12]) {
	Device_List *new_device = (Device_List*)malloc(sizeof(Device_List));
	strcpy(new_device->mac_addr, mac_addr);
	new_device->rssi_list = NULL;
	new_device->next = NULL;
	if (dl == NULL) { //if the list is empty
		dl = &new_device;
	} else { //otherwise we add in tail
		Device_List *temp = *dl;
		while(temp->next != NULL) {
			temp = temp->next;
		}
		temp->next = new_device;
	}
}

///////////////////////////////////////////////
///			Clear the lis of devices		///
///////////////////////////////////////////////
void clear_device_list(Device_List **dl) {
	if((*dl) == NULL)
		return;
	else {
		clear_device_list(&(*dl)->next);
		free(*dl);
	}
	/*/ other way to do it
	Device_List *temp;
	temp = *l;
	while (temp->dl_next != NULL) {
		*l = temp->dl_next;
		free(temp);
	}
	//*/
}

///////////////////////////////////////////
///			Adding a rssi sample		///
///////////////////////////////////////////
void add_rssi_sample(Device_List *l, int rssi_value, int expDate) {
	Rssi_List *rssi_list = (Rssi_List*)malloc(sizeof(Rssi_List));
	rssi_list->rssi_value = rssi_value;
	rssi_list->exp_date = expDate;
	rssi_list->next = NULL;

	if(l->rssi_list == NULL) {
		l->rssi_list = rssi_list;
		l->rssi_list->next = NULL;
		return;
	} else {
		Rssi_List * temp_list = l->rssi_list;
		while(temp_list->next != NULL) {
			temp_list = temp_list->next;
		}
		temp_list->next = rssi_list;
		temp_list->next->next = NULL;
	}	
}

///////////////////////////////////////
///			Clear the rssi list		///
///////////////////////////////////////
void clear_rssi_list(Device_List *l) {
	Rssi_List * rssi_list = l->rssi_list;
	Rssi_List * temp_list;
	while(rssi_list != NULL) {
		temp_list = rssi_list->next;
		free(rssi_list);
		rssi_list = temp_list;
	}
	l->rssi_list = NULL;
}

///////////////////////////////////////////////////
///			Screen display of a mac adress		///
///////////////////////////////////////////////////
void print_Mac_addr(char mca[12]) {
	printf("Mac Address: ");
	printf(mca);
	printf("\n");
}

///////////////////////////////////////////////////////////////////
///			Compute the time difference between 2 values		///
///////////////////////////////////////////////////////////////////
double getTimeDiff(struct timeval *current_time, struct timeval *rssi_time) {
	struct timeval *result = NULL;

	/* Perform the carry for the later subtraction by updating y. */
	if (current_time->tv_usec < rssi_time->tv_usec) {
		int nsec = (rssi_time->tv_usec - current_time->tv_usec) / 1000000 + 1;
		rssi_time->tv_usec -= 1000000 * nsec;
		rssi_time->tv_sec += nsec;
	}
	if (current_time->tv_usec - rssi_time->tv_usec > 1000000) {
		int nsec = (rssi_time->tv_usec - rssi_time->tv_usec) / 1000000;
		rssi_time->tv_usec += 1000000 * nsec;
		rssi_time->tv_sec -= nsec;
	}

	/* Compute the time remaining to wait.
	tv_usec is certainly positive. */
	result->tv_sec = current_time->tv_sec - rssi_time->tv_sec;
	result->tv_usec = current_time->tv_usec - rssi_time->tv_usec;

	/* Return 1 if result is negative. */
	return result->tv_sec;
}

///////////////////////////////////////////////
///			Clear the outdated devices		///
///////////////////////////////////////////////
void delete_outdated(Device_List *l, int currentTime) {
	if(l->rssi_list == NULL) {
		return;
	} else {
		Rssi_List * rssi_list = l->rssi_list;	
		Rssi_List * previous = NULL;
		while(rssi_list != NULL) {
			if(rssi_list->exp_date <= currentTime) {
				if(previous == NULL) {
					l->rssi_list = rssi_list->next;
					free(rssi_list);
					rssi_list = l->rssi_list;
				} else {
					previous->next = rssi_list->next;
					free(rssi_list);
					rssi_list = previous->next;
				}
			} else {
				previous = rssi_list;
				rssi_list = rssi_list->next;
			}
		}
	}	
}

///////////////////////////////////////////////
///			Return a device from the list	///
///////////////////////////////////////////////
Device_List * getDevice(Device_List *l, char mac_addr[12]) {
	Device_List *device_list = l;
	while(device_list != NULL) {
		if(strcmp(device_list->mac_addr, mac_addr)) {
			return device_list;
		}
		device_list = device_list->next;
	}
	return NULL;
}

///////////////////////////////////////////////////////
///			Return the last device from the list	///
///////////////////////////////////////////////////////
Device_List * getLastDevice(Device_List **dl) {
	Device_List *device_list = *dl;
	while(device_list->next != NULL) {
		device_list = device_list->next;
	}
	return device_list;
}

///////////////////////////////////////////////
///			Screen display of a device		///
///////////////////////////////////////////////
void printDevice(Device_List *l) {
	Rssi_List *rssi_list = l->rssi_list;
	printf("Mac Address ="); 
	printf(l->mac_addr);
	printf("\n");

	while(rssi_list != NULL) {
		printf("Rssi = %d ", rssi_list->rssi_value);
		printf("Date = %d \n", rssi_list->exp_date);
		rssi_list = rssi_list->next;
	}
}

///////////////////////////////////////////////
///			Is the device in the list		///
///////////////////////////////////////////////
int deviceExistInList(Device_List *l, char mac_addr[12]) {
	Device_List * device_list = l;
	while(device_list !=NULL) {
		if(strcmp(device_list->mac_addr, mac_addr)) {
			return 1;
		}
		device_list = device_list->next;
	}
	return 0;
}

///////////////////////////////////////////////////////////////////
///			Compute the mean of the rssi values of a device		///
///////////////////////////////////////////////////////////////////
float getMoy(Device_List *l, char mac_addr[12]) {
	float rssi_moy = 0;
	int number = 0;
	Device_List *temp = (Device_List*)malloc(sizeof(Device_List));
	temp = l; 
	while (temp->next != NULL) {
		if(strcmp(temp->mac_addr, mac_addr)) {
			number ++;
			rssi_moy += temp->rssi_list->rssi_value;
		}
		temp = temp->next;
	}
	return (rssi_moy/number);	
}

///////////////////////////////////////////////////////
///			Screen display for the UDP packet		///
///			This method is deprecated				///
///////////////////////////////////////////////////////
void echo( int sd ) {
    int len,n;
    char bufin[MAXBUF];
    struct sockaddr_in remote;

    // need to know how big address struct is, len must be set before the call to recvfrom!!! 
    len = sizeof(remote);

    while (1) {
		// read a datagram from the socket (put result in bufin) 
		n=recvfrom(sd,bufin,MAXBUF,0,(struct sockaddr *)&remote,&len);

		// print out the address of the sender 
		printf("Got a datagram from %s port %d\n",
		 inet_ntoa(remote.sin_addr), ntohs(remote.sin_port));

		if (n<0) {
			perror("Error receiving data");
		} else {
			printf("GOT %d BYTES\n",n);
			// Got something, just send it back
			sendto(sd,bufin,n,0,(struct sockaddr *)&remote,len);
        }
    }
}

///////////////////////////////////////////////
///											///
///			UDP listening					///
///											///
///////////////////////////////////////////////
static void * udp_listening(void *ptr) {
	char *message;
	message = (char *) ptr;
	printf("%s \n", message);
	
	#define MYPORT 7777    // Le port de connection pour l'utilisateur
	#define THEIRPORT 9999  // Le port ou l'on va se connecter
    #define MAXBUFLEN 5000  //taille max du buffer
	
	int sockfd, sockbw;
	struct sockaddr_in my_addr;    // mon adresse 
	struct sockaddr_in their_addr; // Adresse du connecté
	int addr_len, numbytes;
	char buf[MAXBUFLEN];

	if ((sockfd = socket(AF_INET, SOCK_DGRAM, 0)) == -1) {  // We create the socket
		perror("socket");
		exit(1);
	}

	my_addr.sin_family = AF_INET;         // host byte order
	my_addr.sin_port = htons(MYPORT);     // short, network byte order
	my_addr.sin_addr.s_addr = INADDR_ANY; // auto-fill with my IP
	bzero(&(my_addr.sin_zero), 8);        // zero pour le reste de struct

	if (bind(sockfd, (struct sockaddr *)&my_addr, sizeof(struct sockaddr)) == -1) { //we bind the socket
		perror("bind");
		exit(1);
	}
	
	addr_len = sizeof(struct sockaddr);

	while (1) { //listening
		printf("waiting now... \n");
		if ((numbytes=recvfrom(sockfd, buf, MAXBUFLEN, 0, (struct sockaddr *)&their_addr, &addr_len)) == -1) { //wait for reception
		    perror("recvfrom");
		    exit(1);
		}

		printf("reçu un paquet de %s\n", inet_ntoa(their_addr.sin_addr));
		printf("le paquet fait %d octets de long\n",numbytes);
		buf[numbytes] = '\0';
		
		
		//>>>>>>>>>>>>>>>>>>>>>>>  Parsing the buffer <<<<<<<<<<<<<<<<<<<<<<<<
		char *X=NULL;
		char *Y=NULL;
		char mapID[1] = "0";
		//char *macAdd;
		
		char *mess;
		if (numbytes < 22) { //positioning packet
			//here we are cheating a little bit, we should parse the packet
			mess = "RSSI;5C:59:48:02:EA:C2;00:25:9C:3C:20:ED;-32";
		} else { //calibration packet
			mess = "RSSI;186;481;1;5C:59:48:02:EA:C2;00:25:9C:3C:20:ED;-32";
		}
		
		if (numbytes > 22) {  //we are in the calibration mode
			printf("Calibration");
			int ii=4;
			while (ii<numbytes) {
				//>>>>>>>>>>>>>>>>>> parse the data here <<<<<<<<<<<<<<<<<<<<<<<
				/*/
				int index = 0;		
				while(strcmp(";", &buf[ii]) != 0) {
					strcpy(&X[index], &buf[ii]);
					index++;
				}
				index = 0;		
				while(strcmp(";", &buf[ii]) != 0) {
					strcpy(&Y[index], &buf[ii]);
					index++;
				}
				index = 0;		
				while(strcmp(";", &buf[ii]) != 0) {
					strcpy(&mapID[index], &buf[ii]);
					index++;
				}
				//*/
				printf("%d", ii);
				ii++;
			}		
		}
		
		printf("Le paquet contient \"%s\"\n\n",buf);

		if ((sockbw = socket(AF_INET, SOCK_DGRAM, 0)) == -1) { //We create the answer socket
		    perror("socket");
		    exit(1);
		}
		their_addr.sin_family = AF_INET;         // host byte order 
		their_addr.sin_port = htons(THEIRPORT);     // short, network byte order 
		bzero(&(their_addr.sin_zero), 8);        // zero pour le reste de struct 
		
		//We send the answer = the mean of the rssi values for the mac adress
		 if ((numbytes=sendto(sockfd, mess, strlen(mess), 0, (struct sockaddr *)&their_addr, sizeof(struct sockaddr))) == -1) { 
			    perror("sendto");
			    exit(1);
			}
		close(sockbw); //We close the answer socket
	}
    close(sockfd); //We close the communication socket
	pthread_exit(0); //We exit the thread
}

///////////////////////////////////////////////
///											///
///			RSSI listening					///
///											///
///////////////////////////////////////////////
static void * rssi_listening(void *ptr) {
	char *message;
	message = (char *) ptr;
	printf("%s \n", message);

	void *ret = malloc(sizeof(int));	
	char errbuf[PCAP_ERRBUF_SIZE];
	char *dev;
	char mca [1];
	pcap_t *handle;
	Device_List **device_list = NULL; //we define our pointer of pointer of devices

	dev = "prism0"; //this is the interface for siffing packets
	//we create our packet handler
	handle = pcap_open_live(dev, BUFSIZ, 1, 1000, errbuf); 
	if ( handle == NULL ) {
		printf("Could not open pcap on interface\n");
		(*(int*)ret) = -1; 
		pthread_exit(ret);
	}
	
	
	while (1) { //We listen to the packets
		packet = pcap_next (handle , &header ) ;
		if ( ((unsigned int *) packet)[0] == 0x41 ) {
			ph = (struct prism_header *) packet;
			eh = (struct ieee80211_header *) (packet + ph->msglen);
			// Check if FromDS flag equals 0
			if ( (eh->frame_control & 0xc0) == 0x80 ) { //if it's a rssi packet

				sprintf(mca, "%02X%02X%02X%02X%02X%02X",
						eh->source_addr[0],
						eh->source_addr[1],
						eh->source_addr[2],
						eh->source_addr[3],
						eh->source_addr[4],
						eh->source_addr[5]);
				
				//*/
				printf("\nGrabbed packet of length %d\n",header.len);
				printf("Recieved at ..... %s",ctime((const time_t*)&header.ts.tv_sec)); 
				printf("Mac adress : %02X:%02X:%02X:%02X:%02X:%02X\n",
						eh->source_addr[0],
						eh->source_addr[1],
						eh->source_addr[2],
						eh->source_addr[3],
						eh->source_addr[4],
						eh->source_addr[5]);
				printf("Dev name : %02X:%02X:%02X:%02X:%02X:%02X\n",
						ph->devname[0],
						ph->devname[1],
						ph->devname[2],
						ph->devname[3],
						ph->devname[4],
						ph->devname[5]);
				printf("Value : %d  dB\n\n", (ph->rssi).data);
				//*/
				
				
				//>>>>>>>>>>>>>>>>>>>>  Here we add a packet to the list <<<<<<<<<<<<<<<<<
				/*/
				add_device(&device_list, mca);
				add_rssi_sample(&device_list, (ph->rssi).data, header.ts.tv_sec);
				//>>>>>>>>>>>>>>>>>>>>  We also delete the outdated devices <<<<<<<<<<<<<<
				delete_outdated(getDevice(device_list, &mca), 3); //current time
				//*/
			}
		}
	}
	
	pcap_close(handle); //We close the handler
	pthread_exit(ret); //We exit the thread
}

///////////////////////////////////////////////
///											///
///			MAIN program					///
///											///
///////////////////////////////////////////////
int main(int argc, char *argv[]) {
	pthread_t rssi_thread, udp_thread; //our 2 threads for listen to UDP and RSSI
	int rssi_th_id, udp_th_id;
	char *mess_udp = "UDP";
    char *mess_rssi = "RSSI";
	
	//struct thread_params tp;
	int * my_ret = 0;
	
	//We define our 2 threads
	udp_th_id = pthread_create(&udp_thread, NULL, udp_listening, (void*) mess_udp);//(void *) &tp );
	rssi_th_id = pthread_create(&rssi_thread, NULL, rssi_listening, (void*) mess_rssi);
	
	sleep(1); //We wait a little bit
	//and launch the threads
	pthread_join(udp_thread, NULL); //void(**) &my_ret);
	pthread_join(rssi_thread, NULL); //void(**) &my_ret);
	
	printf("%d",*my_ret);
	return(0);
}
